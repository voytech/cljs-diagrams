(ns cljs-diagrams.core.project
 (:require [cljs-diagrams.core.utils.dom :as dom]
           [cljs-diagrams.core.utils.dnd :as dnd]
           [cljs-diagrams.core.utils.commons :as commons]
           [cljs-diagrams.core.tools :as t]
           [cljs-diagrams.core.behaviours :as behaviours]
           [cljs-diagrams.core.layouts :as layouts]
           [cljs-diagrams.core.eventbus :as b]
           [cljs-diagrams.core.events :as events]
           [cljs-diagrams.impl.std.extensions.resolvers.default :as resolvers]
           [cljs-diagrams.core.state :as state]
           [cljs-diagrams.core.entities :as e]
           [cljs-diagrams.core.edn :as edn]
           [cljs-diagrams.core.rendering :as r]))

(defn edn [app-state]
  (->> (state/get-in-diagram-state app-state [:entities])
       vals
       (mapv edn/export-entity)
       prn-str))

(defn save [app-state]
  (console.log (clj->js (edn app-state)))
  (commons/save-to-storage "diagram" (edn app-state)))

(defn get-managed-entities [app-state]
  (vals (state/get-in-diagram-state app-state [:entities])))

(defn refresh-components [app-state]
  (let [entities (get-managed-entities app-state)
        components (reduce #(concat %1 (vals (:components %2))) [] entities)
        c-map (apply merge (mapv (fn [e] {(:uid e) e}) components))]
    (state/assoc-diagram-state app-state [:components] c-map)))

(defn autowire-behaviours [app-state]
  (doseq [entity (get-managed-entities app-state)]
    (behaviours/autowire app-state entity)))

(defn reload-entities [app-state entities]
  (let [reloaded (mapv #(edn/load-entity app-state %) entities)]
    (state/assoc-diagram-state app-state [:entities] (apply merge (mapv (fn [e] {(:uid e) e}) reloaded)))))

(defn load [app-state]
  (let [entities (-> (commons/load-from-storage "diagram")
                     (cljs.reader/read-string))]
    (reload-entities app-state entities)
    (refresh-components app-state)
    (autowire-behaviours app-state)
    (b/fire app-state "diagram.render")))

(defn initialize [id app-state config]
  (dom/console-log (str "Initializing cljs-diagrams within DOM node with id [ " id " ]."))
  (console.log "Initializing event-bus ...")
  (b/initialize app-state)
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
