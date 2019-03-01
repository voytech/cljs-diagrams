(ns cljs-diagrams.core.project
 (:require [cljs-diagrams.core.utils.dom :as dom]
           [cljs-diagrams.core.utils.dnd :as dnd]
           [cljs-diagrams.core.utils.commons :as commons]
           [cljs-diagrams.core.tools :as t]
           [cljs-diagrams.core.behaviours :as behaviours]
           [cljs-diagrams.core.layouts :as layouts]
           [cljs-diagrams.core.eventbus :as b]
           [cljs-diagrams.core.events :as events]
           [cljs-diagrams.impl.extensions.resolvers.default :as resolvers]
           [cljs-diagrams.core.state :as state]
           [cljs-diagrams.core.entities :as e]
           [cljs-diagrams.core.rendering :as r]))

(defn normalize-component [component]
  (-> (into {} component)
      (assoc  :model (-> component :model deref))
      (dissoc :property-change-callback)))

(defn normalize-entity [entity]
  (let [components (vals (:components entity))]
    (-> (into {} entity)
        (assoc :components (mapv normalize-component components)))))

(defn edn [app-state]
  (->> (state/get-in-diagram-state app-state [:entities])
       vals
       (mapv e/normalize)
       prn-str))

(defn save [app-state]
  (commons/save-to-storage "diagram" (edn app-state)))

(defn recreate-components [app-state entities]
  (let [components (reduce #(concat %1 (vals (:components %2))) [] entities)
        c-map (group-by :uid components)]
    (state/assoc-diagram-state app-state [:components] c-map)))

(defn reload-entities [app-state entities]
  (let [denormalized (mapv e/denormalize entities)]
    (state/assoc-diagram-state app-state [:entities] (group-by :uid denormalized))))

(defn load [app-state]
  (console.log (commons/load-from-storage "diagram"))
  (let [entities (-> (commons/load-from-storage "diagram") (cljs.reader/read-string))]
    (recreate-components app-state entities)
    (reload-entities app-state entities)
    (b/fire app-state "diagram.render")))

(defn enable-snapshots [app-state]
  (b/on app-state ["state.save"] -999 (fn [event] (save (-> event :context :app-state)))))

(defn initialize [id app-state config]
  (dom/console-log (str "Initializing cljs-diagrams within DOM node with id [ " id " ]."))
  (console.log "Initializing event-bus ...")
  (b/initialize app-state)
  (enable-snapshots app-state)
  (console.log "Initializing behaviours ...")
  (behaviours/initialize app-state)
  (doseq [install (:behaviours config)]
    (console.log (str "Installed behaviour: " (clj->js (install app-state)))))
  (console.log "Initializing layouts")
  (layouts/initialize app-state)
  (console.log "Initializing renderer ...")
  (r/create-renderer app-state id (:width config) (:height config) (:renderer config))
  (console.log "Initializing resolvers ...")
  (resolvers/initialize app-state)
  (console.log "Dispatching events ...")
  (events/dispatch-events app-state))

;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event state]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)]
    (t/invoke-tool tool-obj state context)))
