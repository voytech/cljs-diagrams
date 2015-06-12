(ns core.actions-impl
  (:require [core.actions :refer [raise Action run-actions next-status debug-action]]
            [utils.dom.dom-utils :as dom]
            [core.settings :as settings]
            [core.canvas-interface :refer [project]]))

(declare change-settings!)

(defn- timestamp [] (str (.getTime (new js/Date))))

(defn action-wrapper [action-code action-data undo-func undo-buff status]
  (Action. (timestamp)
          action-code
          (if (not (nil? status)) status :NEW)
          action-data
          undo-func
          undo-buff
          (dom/time-now)))



(defn new-settings-action-wrapper [val path status]
  (Action. (timestamp)
          :settings-action
          (if (not (nil? status)) status :NEW)
          {:value val :path path}
          (fn [act]
            (raise (new-settings-action-wrapper (:undo-buffer act) path (:status act)))
            )
          (get-in @settings/settings path)
          (dom/time-now)))


(defn change-settings! [val & path]
 (raise (new-settings-action-wrapper val path :NEW)))

(defn- *change-page!* [page-num action-status]
  (raise (action-wrapper :change-page-action
                         page-num
                         (fn [action]
                           (*change-page!* (:undo-buffer action) (:status action)))
                         (get-in @project [:page-index])
                         action-status)))


(defn change-page! [page-num]
  (*change-page!* page-num :NEW))

(defn *change-property!* [entity key value status])

(defn change-property! [entity key value]
  (*change-property!* entity key value :NEW))



(run-actions)
;;register formula side-effect cell for listening to actions list.
