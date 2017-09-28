(ns core.events
  (:require [cljsjs.rx]
            [core.eventbus :as b]
            [core.entities :as e]
            [core.drawables :as d]))


(defonce event-map {"mousedown"  "mousedown"
                    "mouseup"    "mouseup"
                    "click"      "mouseclick"
                    "dbclick"    "mousedbclick"
                    "mousemove"  "mousemove"
                    "mouseenter" "mouseenter"
                    "mouseleave" "mouseleave"})

(defonce patterns (atom {}))

(defonce funcs (atom {}))

(defonce indices (volatile! {}))

(defonce context (volatile! {}))

(defonce state (atom {}))

(defonce phases (volatile! {}))

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

(defn event-name [entity-type attribute-name component-type event-type]
  (let [event-descriptor (clojure.string/join "" (cond-> []
                                                   (not (nil? entity-type)) (conj "e")
                                                   (not (nil? attribute-name)) (conj "a")
                                                   (not (nil? component-type)) (conj "c")))]
    (clojure.string/join "." (cond-> [event-descriptor]
                               (not (nil? entity-type)) (conj (name entity-type))
                               (not (nil? attribute-name)) (conj (name attribute-name))
                               (not (nil? component-type)) (conj (name component-type))
                               (not (nil? event-type)) (conj (name event-type))))))

(defn loose-event-name [entity-type attribute-name component-type event-type]
 (let [has-attribute  (and (not (nil? attribute-name)) (not (nil? entity-type)))]
    (event-name (if has-attribute nil entity-type)
                attribute-name
                component-type
                event-type)))

(defn- lookup-all [x y]
  (->> @d/drawables
       vals
       (filter #(d/contains-point? % x y))
       (sort-by #(d/getp % :z-index) >)))

(defn- normalise-event-type [event]
  (get event-map event))

(defn enrich [drawable]
  (when (d/is-drawable (:uid drawable))
    (let [entity             (e/lookup drawable :entity)
          component          (e/lookup drawable :component)
          attribute-value    (e/lookup drawable :attribute)
          drawable           (:drawable component)]
        {:entity           entity
         :attribute-value  attribute-value
         :drawable         drawable
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
                 (->> (enrich (or (:drawable @state) (first (lookup-all (:left e) (:top e)))))
                      (merge e)))))

(defn- dispatch-events [id events]
  (let [obj (js/document.getElementById id)
        stream (merge-streams obj events)
        onstart    (.map stream (fn [e] (on-phase :start) e))
        normalized (.map onstart (fn [e] (normalise-event e obj)))
        delta    (delta-stream normalized (fn [acc e] {:movement-x (- (:left e) (or (:left acc) 0))
                                                       :movement-y (- (:top e) (or (:top acc) 0))}))
        enriched (enriching-stream delta)
        pattern  (.map enriched (fn [e] (test e)))
        last     (.map pattern  (fn [e] (merge e @state {:type (or (:state @state) (:type e))})))] ; this could be moved to events/tests at the end

      (.subscribe last  (fn [e]
                          (let [event-name (loose-event-name (-> e :entity :type)
                                                             (-> e :attribute-value :attribute :name)
                                                             (-> e :component :type)
                                                             (-> e :type))]
                           (js/console.log (str "on " event-name))
                           (js/console.log (clj->js (-> e :entity)))
                           (b/fire event-name e))))))
