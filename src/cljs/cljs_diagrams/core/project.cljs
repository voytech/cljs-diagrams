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
           [cljs-diagrams.core.rendering :as r]))

;; ====
;; move all global state for entities, event-bus, behaviours, components into this function as local mutable atoms.
;; ===

(defn normalize-component [component]
  (-> (assoc component :model (-> component :model deref))
      (dissoc :property-change-callback)))

(defn normalize-entity [entity]
  (assoc entity :components (mapv normalize-component (vals (:components entity)))))

(defn edn [app-state]
  (->> (state/get-diagram-state app-state [:entities])
       vals
       (mapv normalize-entity)
       prn-str))

(defn save [app-state]
  (commons/save (str "diagram-" (.getTime (js/Date.))) (edn app-state)))

(defn load [name app-state]
  (let [data (commons/load name)]))

(defn enable-state-persist [app-state]
  (b/on app-state ["state.save"] -999 (fn [event]
                                        (let [context (:context event)
                                              app-state (:app-state context)]
                                          (save app-state)))))

(defn initialize [id app-state config]
  (dom/console-log (str "Initializing cljs-diagrams within DOM node with id [ " id " ]."))
  (console.log "Initializing event-bus ...")
  (b/initialize app-state)
  (enable-state-persist app-state)
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
