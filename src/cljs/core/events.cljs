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
(defn- processed [event]
  (let [processed (Event. (:uid event)
                          (:event-code) event
                          (:status :PROCESSED)
                          (:payload event)
                          (:undo-func event)
                          (:undo-buffer event)
                          (:timestamp (new java.util.Date)))]
    (swap! events (fn [lst item] (conj (rest lst) item)) processed )))

(defn- manage-events [events]
  (let [new-events (filter #(= (:status %) :NEW) @events)
        first (first new-events)]
    (when (not (nil? first))
      (on first)
      (processed first))))

(defn run-events []
  (cell= (manage-events events)))

(defn undo [])

(defn redo [])
