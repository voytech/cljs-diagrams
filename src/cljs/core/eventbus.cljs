(ns core.eventbus)

(defonce DEFAULT_PRIORITY 999)

(defonce EVENT_STORE_CAPACITY 20)

(defonce bus (atom {}))

(defonce after (atom {}))

(defonce event-store (volatile! []))

(defn add-event [event]
  (when (> (count @event-store) EVENT_STORE_CAPACITY)
    (vswap! event-store subvec 1))
  (vswap! event-store conj event))

(defn prev-event []
  (last @event-store))

(defn is-listener [name]
  (not (nil? (get @bus name))))

(defn after-all [event-name handler]
  (swap! after assoc event-name handler))

(defn- do-after-all [event-name]
  (when-let [handler (get @after event-name)]
    (handler)))

(defn on
  ([event-names priority callback]
   (doseq [name event-names]
     (let [listeners (or (get @bus name) [])]
       (swap! bus assoc name (->> (conj listeners {:priority priority :callback callback})
                                  (sort-by :priority))))))

  ([event-names callback]
   (on event-names DEFAULT_PRIORITY callback)))

(defn off [event-names]
  (doseq [name event-names]
    (swap! bus dissoc name)))

(defn once [event-name callback]
  (let [wrapper (fn [event]
                  (off [event-name])
                  (callback event))]
     (on [event-name] wrapper)))

(defn- make-event [event-name context]
  {:type      event-name
   :originalEvent nil
   :context   context
   :timestamp (.getTime (js/Date.))
   :uid       0;(str (random-uuid)) ; this is very expensive operation for such latency
   :cancelBubble false
   :defaultPrevented false})

(defn prevent-default [event]
  (vswap! event assoc :defaultPrevented true)
  (when-not (nil? (:originalEvent @event))
    (.preventDefault (:originalEvent @event))))

(defn stop-propagation [event]
  (vswap! event assoc :cancelBubble true)
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
  ([name context]
   (let [event (volatile! (make-event name context))
         listeners (get @bus name)]
     (add-event @event)
     (when-not (nil? listeners)
       (next listeners event)
       (do-after-all event))))

  ([name]
   (fire name {})))
