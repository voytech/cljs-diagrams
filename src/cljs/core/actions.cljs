;;CHange this module to actions.cljs
(ns core.actions
  (:require [utils.dom.dom-utils :as dom]
            [tailrecursion.javelin :refer [cell destroy-cell!]]
            )
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(def history-length 50)

(def manage (atom true))

(defrecord Action [uid
                  action-code
                  status
                  payload
                  undo-func
                  undo-buffer
                  timestamp])

(declare manage-actions)

(def actions (cell (vector)))


(defn raise [action]
  (swap! actions conj action))

(defmulti on :action-code)

(defmethod on :default [action]
  (dom/console-log (str "No action handler registered for action code "
                        (:action-code action))))
(defn- index-of [coll v]
  (let [i (count (take-while #(not= v %) coll))]
    (when (or (< i (count coll))
            (= v (last coll)))
      i)))

(defn debug-action [trace action]
  (dom/console-log (str trace " : " (.stringify js/JSON (clj->js action)))))

;;ACHTUNG this method changes cell actions which may occur in formula re-evaluation!
(defn- change-status [action new-status]
  (let [processed (index-of @actions action)]
    (debug-action "change status " action)
    (swap! actions assoc-in [processed :status] new-status)
    (debug-action "changed status" (get-in @actions [processed]))
))

(defn next-status [action]
  (cond
   (= :NEW (:status action))    :DONE
   (= :DONE (:status action))   :UNDO
   (= :UNDO (:status action))   :UNDONE
   (= :UNDONE (:status action)) :REDO
   (= :REDO (:status action))   :REDONE
   (= :REDONE (:status action)) :UNDO))

(defn- manage-actions [actions]
  (when (= true @manage)
    (let [new-actions (filter #(or (= (:status %) :NEW)
                                   (= (:status %) :UNDO)
                                   (= (:status %) :REDO)) actions)
          head (last new-actions)]
      (when (not (nil? head))
        (debug-action "manage before " head)
        (on head)
        (change-status head (next-status head))
        ))))

(defn run-actions []
  (def actions-formula (cell= (manage-actions actions)))
)

(defn stop-actions []
  (when (not (nil? actions-formula))
    (destroy-cell! actions-formula)))



(defn- del [action]
  (let [new-actions (remove #(= % action) @actions)]
      (reset! actions new-actions))
  (println (count @actions))
  )

(defn *undo* [from-status]
   (let [processed-actions (filter #(contains? from-status (:status %)) @actions)
         action (last processed-actions)]
       (when (not (nil? action))
         (let [undofn (:undo-func action)]
           ;;(stop-actions)
           ;(del action)
           (debug-action "undo " action)
           (change-status action :ARCH) ;;Why the fuck cant just remove
           (when (not (nil? undofn))
             (undofn (assoc action :status (next-status action)))
             ;(run-actions)
           )
           )
         )))

(defn undo [] (*undo* #{:DONE :REDONE}))
(defn redo [] (*undo* #{:UNDONE}))

(defn clear-history []
  (reset! actions (vector)))
