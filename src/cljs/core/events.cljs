(ns core.events)

(defonce events ["mouseover" "mouseout" "mousein" "mouseup" "mousedown" "mousedrag" "dbclick" "click"])

(defonce patterns (atom {}))

(defonce funcs (atom {}))

(defonce indices (atom {}))

(defonce matches (atom []))

(defonce context (atom {}))

(defonce state (atom {}))

(defonce phases (atom {}))

(defn schedule [function phase]
  (let [hooks (cons function (or (phase @phases) []))]
    (swap! phases assoc phase hooks)))

(defn on-phase [phase]
  (let [hooks (phase @phases)]
    (swap! phases dissoc phase)
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
      (= :success result) (swap! indices assoc key count)
      (and (= true result) (< index count)) (swap! indices assoc key (inc index))
      :else (swap! indices assoc key 0))))

(defn- after-match [key event]
  (swap! matches conj key)
  (swap! indices assoc key 0)
  (swap! state assoc :state (name key))
  (swap! state merge ((key @funcs) event)))

(defn- update-test [key event]
   (advance key event)
   (when (matches? key) (after-match key event)))

(defn add-pattern [name step-functions result-function]
  (swap! patterns assoc name step-functions)
  (swap! funcs assoc name result-function)
  (swap! indices assoc name 0))

(defn set-context [event-type data]
  (swap! context assoc event-type data))

(defn get-context [event-type]
  (get @context event-type))

(defn remove-context [event-type]
  (let [data (get-context event-type)]
    (swap! context dissoc event-type)
    data))

(defn has-context [event-type]
  (not (nil? (get-context event-type))))

(defn test [event]
  (doseq [key (keys @patterns)]
     (update-test key event))
  event)
