(ns core.events
  (:require [cljsjs.rx]
            [core.eventbus :as b]
            [core.entities :as e]
            [core.components :as d]))


(defonce events-bindings (atom {}))

(defonce application-events-bindings (atom {}))

(defonce  event-codes {"noop"     {:desc "No operation event code"}
                       "move"     {:desc "Event code reserved for moving components"}
                       "focus"    {:desc "Event should be triggered when entity should get focus"}
                       "blur"     {:desc "Event should be triggered when entity should loose focus"}
                       "select"   {:desc "Event should be triggered when entity has been activated and activation state should be preserved in project buffer"}
                       "activate" {:decc "Event should be triggered when entity has been activated but activation state should not be preserved"}
                       "unselect" {:desc "Event should be triggered when activation state should be retained"}
                       "delete"   {:desc "Event should be triggered when an element is going to be deleted"}
                       "create"   {:desc "Event should be triggered when creating an element"}})

(defonce patterns (atom {}))

(defonce funcs (atom {}))

(defonce indices (volatile! {}))

(defonce context (volatile! {}))

(defonce state (atom {}))

(defonce phases (volatile! {}))

(defn set-original-events-bindings [bindings]
  (reset! events-bindings bindings))

(defn- normalise-event-type [event]
  (or (get @events-bindings event) event))

(defn set-application-events-bindings [bindings]
  (reset! application-events-bindings bindings))

(defn- convert-to-application-event [event]
  (or (get @application-events-bindings event) event))

(defn schedule [function phase]
  (let [hooks (conj (or (phase @phases) []) function)]
    (vswap! phases assoc phase hooks)))

(defn on-phase [phase]
  (let [hooks (phase @phases)]
    (vswap! phases dissoc phase)
    (doseq [hook hooks] (hook))))

(defn clear-state []
  (reset! state {}))

(defn clear-state-for-next-event []
  (schedule clear-state :started))

(defn- matches? [key]
  (let [steps (key @patterns)
        index (key @indices)]
    (= index (count steps))))

(defn- advance [key event]
  (let [steps (key @patterns)
        index (key @indices)
        step (get steps index)
        result (step event)
        count  (count steps)]
    (cond
      (= :success result) (vswap! indices assoc key count)
      (and (= true result) (< index count)) (vswap! indices assoc key (inc index))
      :else (vswap! indices assoc key 0))))

(defn- after-match [key event]
  (vswap! indices assoc key 0)
  (swap! state assoc :state (name key))
  (swap! state merge ((key @funcs) event)))

(defn- update-test [key event]
   (advance key event)
   (when (matches? key) (after-match key event)))

(defn add-pattern [name step-functions result-function]
  (swap! patterns assoc name step-functions)
  (swap! funcs assoc name result-function)
  (vswap! indices assoc name 0))

(defn set-context [event-type data]
  (vswap! context assoc event-type data))

(defn get-context [event-type]
  (get @context event-type))

(defn remove-context [event-type]
  (let [data (get-context event-type)]
    (vswap! context dissoc event-type)
    data))

(defn has-context [event-type]
  (not (nil? (get-context event-type))))

(defn test [event]
  (doseq [key (keys @patterns)]
     (update-test key event))
  event)

(defn- ns-qualified-element-name [type]
  (str (namespace type) "." (name type)))

(defn event-name [entity-type attribute-name component-type event-type]
  (let [event-descriptor (clojure.string/join "" (cond-> []
                                                   (not (nil? entity-type)) (conj "e")
                                                   (not (nil? attribute-name)) (conj "a")
                                                   (not (nil? component-type)) (conj "c")))]
    (clojure.string/join ":" (cond-> [event-descriptor]
                               (not (nil? entity-type)) (conj (ns-qualified-element-name entity-type))
                               (not (nil? attribute-name)) (conj (ns-qualified-element-name attribute-name))
                               (not (nil? component-type)) (conj (ns-qualified-element-name component-type))
                               (not (nil? event-type)) (conj (name event-type))))))

(defn entity-event-key [entity-type attribute-name component-type event-type]
 (let [has-attribute  (and (not (nil? attribute-name)) (not (nil? entity-type)))]
    (event-name (if has-attribute nil entity-type)
                attribute-name
                component-type
                event-type)))

(defn- resolve-targets [x y]
  (->> @d/components
       vals
       (filter #(d/contains-point? % x y))
       (sort-by #(d/resolve-z-index (d/getp % :z-index)) >)))

(defn enrich [component]
  (when (d/is-component (:uid component))
    (let [entity             (e/lookup component :entity)
          attribute-value    (e/lookup component :attribute)]
        {:entity           entity
         :attribute-value  attribute-value
         :component        component})))

(defn normalise-event [e obj]
  (let [rect (.getBoundingClientRect obj)
        left (- (.-clientX e) (.-left rect))
        top (- (.-clientY e) (.-top rect))]
     {:source e
      :ctrl-key (.-ctrlKey e)
      :target (.-target e)
      :type (normalise-event-type (.-type e))
      :state (or (:state @state) (normalise-event-type (.-type e)))
      :left left
      :top  top
      :movement-x 0
      :movement-y 0}))

(defn- merge-streams [obj events]
  (apply js/Rx.Observable.merge (mapv (fn [e] (js/Rx.Observable.fromEvent obj e)) events)))

(defn- delta-stream [input func]
  (.scan input (fn [acc,e] (merge acc e (func acc e))) {}))

(defn- enriching-stream [input]
  (.map input (fn [e]
                 (->> (enrich (or (:component @state) (first (resolve-targets (:left e) (:top e)))))
                      (merge e)))))

(defn trigger-bus-event
  ([e]
   (let [_e (assoc e :type (convert-to-application-event (:type e)))
         event-name (entity-event-key (-> _e :entity :type)
                                      (-> _e :attribute-value :attribute :name)
                                      (-> _e :component :type)
                                      (-> _e :type))]
     (js/console.log (str "on " event-name " [ total events :" (inc (b/total-events)) " ]"))
     (b/fire event-name _e)))
  ([e overrides]
   (trigger-bus-event (merge e overrides))))

(defn- dispatch-events [id]
  (let [events ["click" "dbclick" "mousemove" "mousedown" "mouseup"
                "mouseenter" "mouseleave" "keypress" "keydown" "keyup"]
        obj        (js/document.getElementById id)
        stream     (merge-streams obj events)
        onstart    (.map stream (fn [e] (on-phase :start) e))
        normalized (.map onstart (fn [e] (normalise-event e obj)))
        delta      (delta-stream normalized (fn [acc e] {:movement-x (- (:left e) (or (:left acc) 0))
                                                         :movement-y (- (:top e) (or (:top acc) 0))}))
        enriched (enriching-stream delta)
        pattern  (.map enriched (fn [e] (test e)))
        last     (.map pattern  (fn [e] (merge e @state {:type (or (:state @state) (:type e))})))] ; this could be moved to events/tests at the end

      (.subscribe last  (fn [e]
                          (on-phase :completed)
                          (trigger-bus-event e)))))
