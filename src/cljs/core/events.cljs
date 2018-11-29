(ns core.events
  (:require [core.eventbus :as b]
            [core.entities :as e]
            [core.components :as d]
            [cljs.core.async :as async :refer [>! <! put! chan alts!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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

;(defonce indices (volatile! {}))

;(defonce context (volatile! {}))

;(defonce state (atom {}))

(defonce phases (volatile! {}))

(defn- normalise-event-type [app-state event]
  (let [bindings (-> @app-state :events :canonical-events)]
    (or (get bindings event) event)))

(defn- convert-to-application-event [app-state event]
  (let [bindings (-> @app-state :events :application-events)]
    (or (get bindings event) event)))

(defn schedule [function phase]
  (let [hooks (conj (or (phase @phases) []) function)]
    (vswap! phases assoc phase hooks)))

(defn on-phase [phase]
  (let [hooks (phase @phases)]
    (vswap! phases dissoc phase)
    (doseq [hook hooks] (hook))))

(defn clear-state [{:keys [state]}]
  (reset! state {}))

(defn clear-state-for-next-event []
  (schedule clear-state :started))

(defn- matches? [{:keys [indices]} key]
  (let [steps (key @patterns)
        index (key @indices)]
    (= index (count steps))))

(defn- advance [{:keys [indices] :as s} key event]
  (let [steps (key @patterns)
        index (or (key @indices) 0)
        step (get steps index)
        result (step event s)
        count  (count steps)]
    (cond
      (= :success result) (vswap! indices assoc key count)
      (and (= true result) (< index count)) (vswap! indices assoc key (inc index))
      :else (vswap! indices assoc key 0))))

(defn- after-match [{:keys [indices state] :as s} key event]
  (vswap! indices assoc key 0)
  (swap! state assoc :state (name key))
  (swap! state merge ((key @funcs) event s)))

(defn- update-test [matching-context key event]
   (advance matching-context key event)
   (when (matches? matching-context key) (after-match matching-context key event)))

(defn add-pattern [name step-functions result-function]
  (swap! patterns assoc name step-functions)
  (swap! funcs assoc name result-function))
  ;(vswap! indices assoc name 0))

(defn set-context [{:keys [context]} event-type data]
  (vswap! context assoc event-type data))

(defn get-context [{:keys [context]} event-type]
  (get @context event-type))

(defn remove-context [{:keys [context] :as s} event-type]
  (let [data (get-context s event-type)]
    (vswap! context dissoc event-type)
    data))

(defn has-context [{:keys [context] :as s} event-type]
  (not (nil? (get-context s event-type))))

(defn test [matching-context event]
  (doseq [key (keys @patterns)]
     (update-test matching-context key event))
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

(defn- enrich [component]
  (when (d/is-component (:uid component))
    (let [entity             (e/lookup component :entity)]
        {:entity           entity
         :component        component})))

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
         _ (console.log (clj->js _e))
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

(defn normalise-chan [matching-context app-state el source-chan]
  (let [output (chan)]
    (go (loop []
          (on-phase :start)
          (let [event (<! source-chan)]
            (put! output (normalise-event (:state matching-context) app-state event el))
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

(defn enriching-chan [{:keys [state]} source-chan]
  (let [output (chan)]
    (go (loop []
          (let [event (<! source-chan)]
            (put! output (->> (enrich (or (:component @state) (first (resolve-targets (:left event) (:top event)))))
                              (merge event)))
            (recur))))
    output))

(defn pattern-test-chan [matching-context source-chan]
  (let [output (chan)]
    (go (loop []
          (let [event (<! source-chan)
                tested (test matching-context event)
                current-state (-> matching-context :state (deref))]
            (put! output (merge tested current-state {:type (or (:state current-state) (:type tested))}))
            (recur))))
    output))

(defn dispatch-events [app-state]
  (let [events ["click" "dbclick" "mousemove" "mousedown" "mouseup"
                "mouseenter" "mouseleave" "keypress" "keydown" "keyup"]
        id         (-> @app-state :dom :id)
        el         (js/document.getElementById id)
        matching-context {:indices (volatile! {})
                          :context (volatile! {})
                          :state   (atom {})}
        sink-chan  (->> (chan)
                        (events->chan el events)
                        (normalise-chan matching-context app-state el)
                        (position-delta-chan)
                        (enriching-chan matching-context)
                        (pattern-test-chan matching-context))]
          (go (loop []
                (let [event (<! sink-chan)]
                  (on-phase :completed)
                  (console.log (clj->js event))
                  (trigger-bus-event event)
                  (recur))))))
