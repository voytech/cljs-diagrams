(ns core.actions-impl
  (:require [core.actions :refer [raise Action run-actions]]
            [utils.dom.dom-utils :as dom]
            [core.settings :as settings]
            [core.canvas-interface :refer [project]]))

(declare change-settings!)

(defn- timestamp [] (str (.getTime (new js/Date))))

(defn action-wrapper [action-code action-data undo-func undo-buff]
  (Action. (timestamp)
          action-code
          :NEW
          action-data
          undo-func
          undo-buff
          (dom/time-now)))


(defn new-settings-action-wrapper [val path]
  (Action. (timestamp)
          :settings-action
          :NEW
          {:value val :path path}
          (fn [action]
            (println (str "Undoing settings to value :" (:undo-buffer action) " on path " path))
            (apply change-settings! (:undo-buffer action) path)) ;;as an undo funciton we should revert settings to undo-buffer value under given path!
          (get-in @settings/settings path)   ;;In undo-buffer place current settings value under given path
          (dom/time-now)))

(defn change-settings! [val & path]
 (raise (new-settings-action-wrapper val path)))

(defn change-page! [page-num]
  (raise (action-wrapper :change-page-action
                        page-num
                        (fn [action]
                          (change-page! (:undo-buffer action)))
                        (get-in @project [:page-index]))))





(run-actions) ;;register formula side-effect cell for listening to actions list.
