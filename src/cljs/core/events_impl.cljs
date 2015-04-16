(ns core.events-impl
  (:require [core.events :refer [raise Event]]
            [utils.dom.dom-utils :as dom]
            [core.settings :as settings]))

(declare change-settings!)

(defn- timestamp [] (str (.getTime (new js/Date))))

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
 (raise (new-settings-event-wrapper val path))
  )
