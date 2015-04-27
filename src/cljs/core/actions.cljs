;;CHange this module to actions.cljs
(ns core.actions
  (:require [utils.dom.dom-utils :as dom]
            [tailrecursion.javelin :refer [cell]])
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(defrecord Action [uid
                  action-code
                  status
                  payload
                  undo-func
                  undo-buffer
                  timestamp])

(def actions (cell '()))

(defn raise [action]
  (swap! actions conj action))

(defmulti on :action-code)

(defmethod on :default [action]
  (dom/console-log (str "No action handler registered for action code "
                        (:action-code action))))

;;ACHTUNG this method changes cell actions which may occur in formula re-evaluation!
(defn- change-status [action new-status]
  (let [processed (Action. (:uid action)    ;;transients ????
                          (:action-code action)
                          new-status
                          (:payload action)
                          (:undo-func action)
                          (:undo-buffer action)
                          (dom/time-now))]
    (swap! actions (fn [lst item] (conj (rest lst) item)) processed )
))

(defn- manage-actions [actions]
  (let [new-actions (filter #(= (:status %) :NEW) actions)
        head (first new-actions)]
    (when (not (nil? head))
      (on head)
      (change-status head :PROCESSED)
      )))

(defn run-actions []
  (cell= (manage-actions actions)))

(defn undo []
  (let [processed-actions (filter #(= (:status %) :PROCESSED) @actions)
        head (first processed-actions)]
    (when (not (nil? head))
      (let [undofn (:undo-func head)]
          (change-status head :UNDONE)
          (undofn head))
      )))

(defn redo [])

(defn clear-history []
  (reset! actions '()))
