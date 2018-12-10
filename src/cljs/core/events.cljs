(ns core.events
  (:require [core.eventbus :as b]
            [core.entities :as e]
            [core.components :as d]
            [cljs.core.async :as async :refer [>! <! put! chan alts!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- normalise-event-type [app-state event]
  (let [bindings (-> @app-state :events :canonical-events)]
    (or (get bindings event) event)))

(defn- convert-to-application-event [app-state event]
  (let [bindings (-> @app-state :events :application-events)]
    (or (get bindings event) event)))

(defn schedule [{:keys [phases] :as processing-state} function phase]
  (let [hooks (conj (or (phase @phases) []) function)]
    (vswap! phases assoc phase hooks)))

(defn on-phase [{:keys [phases] :as processing-state} phase]
  (let [hooks (phase @phases)]
    (vswap! phases dissoc phase)
    (doseq [hook hooks] (hook))))

(defn clear-state [{:keys [state]}]
  (reset! state {}))

(defn clear-state-for-next-event []
  (schedule clear-state :started))

(defn- matches? [{:keys [indices]} key pattern]
  (let [steps (:steps pattern)
        index (key @indices)]
    (= index (count steps))))

(defn- advance [{:keys [indices] :as process-state} key event pattern]
  (let [steps (:steps pattern)
        index (or (key @indices) 0)
        step (get steps index)
        result (step event process-state)
        count  (count steps)]
    (cond
      (= :success result) (vswap! indices assoc key count)
      (and (= true result) (< index count)) (vswap! indices assoc key (inc index))
      :else (vswap! indices assoc key 0))))

(defn- after-match [{:keys [indices state] :as process-state} key event pattern]
  (let [success (:success pattern)]
    (vswap! indices assoc key 0)
    (swap! state assoc :state (name key))
    (swap! state merge (success event process-state))))

(defn- update-test [process-state key event pattern]
   (advance process-state key event pattern)
   (when (matches? process-state key pattern) (after-match process-state key event pattern)))

(defn test [process-state event]
 (doseq [key (keys (-> event :app-state deref :events :patterns))]
    (update-test process-state key event (-> event :app-state deref :events :patterns key)))
 event)

(defn pattern [name step-functions result-function]
  {:name name
   :steps step-functions
   :success result-function})

(defn set-context [{:keys [context]} event-type data]
  (vswap! context assoc event-type data))

(defn get-context [{:keys [context]} event-type]
  (get @context event-type))

(defn remove-context [{:keys [context] :as process-state} event-type]
  (let [data (get-context process-state event-type)]
    (vswap! context dissoc event-type)
    data))

(defn has-context [{:keys [context] :as process-state} event-type]
  (not (nil? (get-context process-state event-type))))

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

(defn- resolve-targets [components x y]
  (->> components
       vals
       (filter #(d/contains-point? % x y))
       (sort-by #(d/resolve-z-index (d/getp % :z-index)) >)))

(defn enrich [event component]
  (when (d/is-component (-> event :app-state) (:uid component))
    (let [entity (e/lookup (-> event :app-state) component)]
      {:entity entity :component component})))

(defn- normalise-event [state app-state e obj]
  (let [rect (.getBoundingClientRect obj)
        left (- (.-clientX e) (.-left rect))
        top (- (.-clientY e) (.-top rect))]
     {:source e
      :app-state app-state
      :ctrl-key (.-ctrlKey e)
      :target (.-target e)
      :type (normalise-event-type app-state (.-type e))
      :state (or (:state @state) (normalise-event-type app-state (.-type e)))
      :left left
      :top  top
      :movement-x 0
      :movement-y 0}))

(defn trigger-bus-event
  ([e]
   (let [_e (assoc e :type (convert-to-application-event (:app-state e) (:type e)))
         event-name (event-name (-> _e :entity :type)
                                (-> _e :component :type)
                                (-> _e :type))]
     (b/fire (:app-state e) event-name _e)))
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

(defn normalise-chan [{:keys [state] :as process-state} app-state el source-chan]
  (let [output (chan)]
    (go (loop []
          (on-phase process-state :start)
          (let [event (<! source-chan)]
            (put! output (normalise-event state app-state event el))
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

(defn enriching-chan [{:keys [state] :as process-state} source-chan]
  (let [output (chan)]
    (go (loop []
          (let [event (<! source-chan)
                components (get-in @(:app-state event) [:diagram :components])]
            (put! output (->> (enrich event (or (:component @state)
                                                (first (resolve-targets components (:left event) (:top event)))))
                              (merge event)))
            (recur))))
    output))

(defn pattern-test-chan [process-state source-chan]
  (let [output (chan)]
    (go (loop []
          (let [event (<! source-chan)
                tested (test process-state event)
                current-state (-> process-state :state (deref))]
            (put! output (merge tested current-state {:type (or (:state current-state) (:type tested))}))
            (recur))))
    output))

(defn dispatch-events [app-state]
  (let [events ["click" "dbclick" "mousemove" "mousedown" "mouseup"
                "mouseenter" "mouseleave" "keypress" "keydown" "keyup"]
        id         (-> @app-state :dom :id)
        el         (js/document.getElementById id)
        process-state {:indices (volatile! {})
                       :context (volatile! {})
                       :state   (atom {})
                       :phases  (volatile! {})}
        sink-chan  (->> (chan)
                        (events->chan el events)
                        (normalise-chan process-state app-state el)
                        (position-delta-chan)
                        (enriching-chan process-state)
                        (pattern-test-chan process-state))]
          (go (loop []
                (let [event (<! sink-chan)]
                  (on-phase process-state :completed)
                  (trigger-bus-event event)
                  (recur))))))
