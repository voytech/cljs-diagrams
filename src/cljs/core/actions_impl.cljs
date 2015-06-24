(ns core.actions-impl
  (:require [core.actions :refer [raise Action run-actions next-status debug-action]]
            [utils.dom.dom-utils :as dom]
            [core.settings :as settings]
            [core.entities.entity :as e]
           ; [core.canvas-interface :refer [project]]
            ))

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

(defn- *change-page!* [page-num current-val action-status]
  (raise (action-wrapper :change-page-action
                         page-num
                         (fn [action]
                           (*change-page!* (:undo-buffer action) (:payload action) (:status action)))
                         current-val;(get-in @in-project [:page-index])
                         action-status)))


(defn change-page! [page-num current-val]
  (*change-page!* page-num current-val :NEW))

(defn *change-property!* [entity-id key value status execute]
   (raise (action-wrapper :change-property-action
                          {:entity-id entity-id
                           :key key
                           :value value
                           :execute execute}
                          (fn [action]
                            (*change-property!* entity-id key (:undo-buffer action) (:status action) true))
                          (e/get-entity-property entity-id key)
                          status))
)

(defn change-property!
  ([entity-id key value execute] (*change-property!* entity-id key value :NEW execute))
  ([entity-id key value] (*change-property!* entity-id key value :NEW true)))

(defn build-property-map [entity-id keys]
  (let [entity (e/entity-by-id entity-id)
        result (atom {})]
    (doseq [prop keys]
      (swap! result assoc-in [prop] (prop (e/data entity)))
      (println (str prop ":" (prop @result)))
      )
    @result)
)

(defn *change-properties!* [entity-id properties-map status execute]
   (println (str "execute " execute))
   (raise (action-wrapper :change-properties-action
                          {:entity-id entity-id
                           :values properties-map
                           :execute execute}
                          (fn [action]
                            (*change-properties!* entity-id (:undo-buffer action) (:status action) true))
                          (build-property-map entity-id (keys properties-map))
                          status))
)

(defn change-properties!
  ([entity-id properties-map execute] (*change-properties!* entity-id properties-map :NEW execute))
  ([entity-id properties-map] (*change-properties!* entity-id properties-map :NEW true)))

(run-actions)
;;register formula side-effect cell for listening to actions list.
