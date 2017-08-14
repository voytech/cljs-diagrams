(ns core.eventbus)

(defonce DEFAULT_PRIORITY 999)

(defonce EVENT_STORE_CAPACITY 20)

(defonce bus (atom {}))

(defonce event-store (atom []))

(defn add-event [event]
  (when (> (count @event-store) EVENT_STORE_CAPACITY)
    (swap! event-store subvec 1))
  (swap! event-store conj event))

(defn prev-event []
  (last @event-store))

(defn on
  ([event-name priority callback]
   (doseq [name event-names]
     (when-let [listeners (get @bus name)]
       (swap! bus assoc name (->> (cons {:priority priority :callback callback} listeners)
                                  (sort-by :priority))))))

  ([event-names callback]
   (on event-names DEFAULT_PRIORITY callback)))

(defn off [event-names]
  (doseq [name event-names]
    (swap! bus dissoc name)))

(defn once [event-name callback]
  (let [wrapper (fn [event]
                  (callback event)
                  (off event-name))]
     (on event-names wrapper)))

(defn- make-event [event-name context]
  {:type      event-name
   :originalEvent nil
   :context   context
   :timestamp (.getTime (js/Date.))
   :uid       (str (random-uuid))
   :cancelBubble false
   :defaultPrevented false})

(defn prevent-default [event]
  (swap! event assoc :defaultPrevented true)
  (when-not (nil? (:originalEvent @event))
    (.preventDefault (:originalEvent @event))))

(defn stop-propagation [event]
  (swap! event assoc :cancelBubble true)
  (when-not (nil? (:originalEvent @event))
    (.stopPropagation (:originalEvent @event))))

(defn- next [listeners event]
  (let [listener (first listeners)]
    (when-not (nil? listener)
      (let [result ((:callback listener) event)]
        (if (nil? result)
          (when (and (not (:cancelBubble @event))
                     (not (:defaultPrevented @event)))
            (recur (rest listeners) event))
          result)))))

(defn fire
  ([event context]
   (let [event (atom (make-event event context))
         listeners (get @bus event)]
     (add-event @event)
     (when-not (nil? listeners)
       (next listeners event))))

  ([event]
   (fire event {})))
