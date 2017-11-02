(ns core.eventbus)

(defonce DEFAULT_PRIORITY 999)

(defonce EVENT_STORE_CAPACITY 20)

(defonce bus (atom {}))

(defonce after (atom {}))

(defonce event-store (volatile! []))

(defonce ^:private event-cnt (volatile! 0))

(defonce ^:private events-pool (volatile! []))

(defn total-events [] @event-cnt)

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
   :uid       (vswap! event-cnt inc)
   :cancelBubble false
   :defaultPrevented false})

(defn- is-available? [event]
  (not (nil? event)))

(defn- append-new-event [event-name context]
  (peek (vswap! events-pool conj (make-event event-name context))))

(defn- borrow-event [event-name context]
  (let [availables (vec (filterv #(is-available? %) @events-pool))
        event (or (peek availables) (append-new-event event-name context))]
     (merge event {:type event-name
                   :context   context
                   :timestamp (.getTime (js/Date.))
                   :uid   (vswap! event-cnt inc)})))

(defn prevent-default [event]
  (when-not (nil? (:originalEvent event))
    (.preventDefault (:originalEvent event))))

(defn stop-propagation [event]
  (when-not (nil? (:originalEvent event))
    (.stopPropagation (:originalEvent event))))

;; as long as listeners are vector we should rather use peek and it should be sorted in reversed order.
(defn- next [listeners event]
  (let [listener (first listeners)]
    (when-not (nil? listener)
      (let [result ((:callback listener) event)]
        (if (nil? result)
          (when (and (not (:cancelBubble event))
                     (not (:defaultPrevented event)))
            (recur (rest listeners) event))
          result)))))

(defn fire
  ([name context]
   (let [event (borrow-event name context)
         listeners (get @bus name)]
     (add-event event)
     (when-not (nil? listeners)
       (let [result (next listeners event)]
         (do-after-all name)
         result))))
  ([name]
   (fire name {})))
