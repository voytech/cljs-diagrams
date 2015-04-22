(ns core.events-impl
  (:require [core.events :refer [raise Event run-events]]
            [utils.dom.dom-utils :as dom]
            [core.settings :as settings]
            [core.canvas-interface :refer [project]]))

(declare change-settings!)

(defn- timestamp [] (str (.getTime (new js/Date))))

(defn event-wrapper [event-code event-data undo-func undo-buff]
  (Event. (timestamp)
          event-code
          :NEW
          event-data
          undo-func
          undo-buff
          (dom/time-now)))


(defn new-settings-event-wrapper [val path]
  (Event. (timestamp)
          :settings-event
          :NEW
          {:value val :path path}
          (fn [event]
            (println (str "Undoing settings to value :" (:undo-buffer event) " on path " path))
            (apply change-settings! (:undo-buffer event) path)) ;;as an undo funciton we should revert settings to undo-buffer value under given path!
          (get-in @settings/settings path)   ;;In undo-buffer place current settings value under given path
          (dom/time-now)))

(defn change-settings! [val & path]
 (raise (new-settings-event-wrapper val path)))

(defn change-page! [page-num]
  (raise (event-wrapper :change-page-event
                        page-num
                        (fn [event]
                          (change-page! (:undo-buffer event)))
                        (get-in @project [:page-index]))))





(run-events) ;;register formula side-effect cell for listening to events list.
