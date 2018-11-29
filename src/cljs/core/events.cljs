(ns core.events
  (:require [cljsjs.rx]
            [core.eventbus :as b]
            [core.entities :as e]
            [core.components :as d]
            [cljs.core.async :as async :refer [>! <! put! chan alts!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


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

(defn event-name [entity-type component-type event-type]
  (let [event-descriptor (clojure.string/join "" (cond-> []
                                                   (not (nil? entity-type))    (conj "e")
                                                   (not (nil? component-type)) (conj "c")))]
    (clojure.string/join ":" (cond-> [event-descriptor]
                               (not (nil? entity-type))    (conj (ns-qualified-element-name entity-type))
                               (not (nil? component-type)) (conj (ns-qualified-element-name component-type))
                               (not (nil? event-type))     (conj (name event-type))))))

(defn- resolve-targets [x y]
  (->> @d/components
       vals
       (filter #(d/contains-point? % x y))
       (sort-by #(d/resolve-z-index (d/getp % :z-index)) >)))

(defn enrich [component]
  (when (d/is-component (:uid component))
    (let [entity             (e/lookup component :entity)]
        {:entity           entity
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
         event-name (event-name (-> _e :entity :type)
                                (-> _e :component :type)
                                (-> _e :type))]
     (b/fire event-name _e)))
  ([e overrides]
   (trigger-bus-event (merge e overrides))))

(defn events->chan [el event-types chan]
  (doseq [event-type event-types]
    (.addEventListener el event-type (fn [e] (put! chan e))))
  chan)

(defn merge-chans [& chans]
  (let [rc (chan)]
    (go
     (loop []
       (put! rc (first (alts! chans)))
       (recur)))
    rc))

(defn filter-chan [pred channel]
  (let [rc (chan)]
    (go (loop []
          (let [val (<! chan)]
            (if (pred val) (put! rc val))
            (recur))))
    rc))

(defn normalise-chan [el source-chan]
  (let [output (chan)]
    (go (loop []
          (on-phase :start)
          (let [event (<! source-chan)]
            (put! output (normalise-event event el))
            (recur))))
    output))

(defn position-delta-chan [source-chan]
  (let [output (chan)]
    (go (loop [prev nil]
          (let [event (<! source-chan)]
            (put! output (merge event {:movement-x (- (:left event) (or (:left prev) 0))
                                       :movement-y (- (:top event)  (or (:top prev) 0))}))
            (recur event))))
    output))

(defn enriching-chan [source-chan]
  (let [output (chan)]
    (go (loop []
          (let [event (<! source-chan)]
            (put! output (->> (enrich (or (:component @state) (first (resolve-targets (:left event) (:top event)))))
                              (merge event)))
            (recur))))
    output))

(defn pattern-test-chan [source-chan]
  (let [output (chan)]
    (go (loop []
          (let [event (<! source-chan)
                tested (test event)]
            (put! output (merge tested @state {:type (or (:state @state) (:type tested))}))
            (recur))))
    output))

(defn event-processing-chan [el source-chan]
  (let [output (chan)]
    (go (loop [prev nil]
          (on-phase :start)
          (let [event (<! source-chan)
                normalised  (normalise-event event el)
                moved       (merge normalised {:movement-x (- (:left normalised) (or (:left prev) 0))
                                               :movement-y (- (:top normalised)  (or (:top prev) 0))})
                enriched    (->> (enrich (or (:component @state)
                                             (first (resolve-targets (:left moved) (:top moved)))))
                                 (merge moved))
                tested      (test enriched)
                fin         (merge tested @state {:type (or (:state @state) (:type tested))})]
            (put! output fin)
            (recur fin))))
    output))

(defn dispatch-events-v2 [id]
  (let [events ["click" "dbclick" "mousemove" "mousedown" "mouseup"
                "mouseenter" "mouseleave" "keypress" "keydown" "keyup"]
        el         (js/document.getElementById id)
        sink-chan  (->> (chan)
                        (events->chan el events)
                        (event-processing-chan el))]
          (go (loop []
                (let [event (<! sink-chan)]
                  (on-phase :completed)
                  (trigger-bus-event event)
                  (recur))))))

(defn dispatch-events [id]
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
