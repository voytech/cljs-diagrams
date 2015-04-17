(ns core.events
  (:require [utils.dom.dom-utils :as dom]
            [tailrecursion.javelin :refer [cell]])
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(defrecord Event [uid
                  event-code
                  status
                  payload
                  undo-func
                  undo-buffer
                  timestamp])

(def events (cell '()))

(defn raise [event]
  (swap! events conj event))

(defmulti on :event-code)

(defmethod on :default [event]
  (dom/console-log (str "No event handler registered for event code "
                        (:event-code event))))

;;ACHTUNG this method changes cell events which may occur in formula re-evaluation!
(defn- change-status [event new-status]
  (let [processed (Event. (:uid event)    ;;transients ????
                          (:event-code event)
                          new-status
                          (:payload event)
                          (:undo-func event)
                          (:undo-buffer event)
                          (dom/time-now))]
    (swap! events (fn [lst item] (conj (rest lst) item)) processed )
))

(defn- manage-events [events]
  (let [new-events (filter #(= (:status %) :NEW) events)
        head (first new-events)]
    (when (not (nil? head))
      (on head)
      (change-status head :PROCESSED)
      )))

(defn run-events []
  (cell= (manage-events events)))

(defn undo []
  (let [processed-events (filter #(= (:status %) :PROCESSED) @events)
        head (first processed-events)]
    (when (not (nil? head))
      (let [undofn (:undo-func head)]
          (change-status head :UNDONE)
          (undofn head))
      )))

(defn redo [])

(defn clear-history []
  (reset! events '()))
