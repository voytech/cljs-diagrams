(ns core.eventbus
  (:require [core.state :as state]))

(defonce DEFAULT_PRIORITY 999)

(defonce EVENT_STORE_CAPACITY 20)

(defn add-event [app-state event]
  (let [event-store (state/get-in-event-bus-state app-state [:event-store])
        trimmed (if (> (count event-store) EVENT_STORE_CAPACITY)
                  (subvec event-store 1)
                  event-store)]
    (state/assoc-event-bus-state app-state [:event-store] (conj trimmed event))))

(defn after-all [app-state event-name handler]
  (state/assoc-event-bus-state app-state [:after event-name] handler))

(defn- do-after-all [app-state event-name]
  (when-let [handler (state/get-in-event-bus-state app-state [:after event-name])]
    (handler)))

(defn on
  ([app-state event-names priority callback]
   (doseq [name event-names]
     (let [listeners (or (state/get-in-event-bus-state app-state [:handlers name]) [])]
       (state/assoc-event-bus-state app-state [:handlers name] (->> (conj listeners {:priority priority :callback callback})
                                                                    (sort-by :priority))))))

  ([app-state event-names callback]
   (on app-state event-names DEFAULT_PRIORITY callback)))

(defn off [app-state event-names]
  (doseq [name event-names]
    (state/dissoc-event-bus-state app-state [:handlers name])))

(defn once [app-state event-name callback]
  (let [wrapper (fn [event]
                  (off app-state [event-name])
                  (callback event))]
     (on app-state [event-name] wrapper)))

(defn- make-event [event-name context]
  {:type      event-name
   :originalEvent nil
   :context   context
   :timestamp (.getTime (js/Date.))
   :cancelBubble false
   :defaultPrevented false})

(defn- is-available? [event]
  (not (nil? event)))

(defn- append-new-event [app-state event-name context]
  (let [event-pool (state/get-in-event-bus-state app-state [:event-pool])]
    (state/assoc-event-bus-state app-state [:event-pool] (conj event-pool (make-event event-name context)))
    (peek (state/get-in-event-bus-state app-state [:event-pool]))))

(defn- borrow-event [app-state event-name context]
  (let [availables (vec (filterv #(is-available? %) (state/get-in-event-bus-state app-state [:event-pool])))
        event (or (peek availables) (append-new-event app-state event-name context))]
     (merge event {:type event-name
                   :context   context
                   :timestamp (.getTime (js/Date.))})))

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
  ([app-state name context]
   (let [event (borrow-event app-state name (merge context {:app-state app-state}))
         listeners (state/get-in-event-bus-state app-state [:handlers name])]
     (add-event app-state event)
     (when-not (nil? listeners)
       (let [result (next listeners event)]
         (do-after-all app-state name)
         result))))
  ([app-state name]
   (fire app-state name {})))

(defn initialize [app-state]
  (state/assoc-event-bus-state app-state [] {
                                             :handlers {}
                                             :after {}
                                             :event-store []
                                             :event-pool []}))
