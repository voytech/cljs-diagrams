(ns core.events)

(defonce events ["mouseover" "mouseout" "mousein" "mouseup" "mousedown" "mousedrag" "dbclick" "click"])

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
