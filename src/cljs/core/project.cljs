(ns core.project
 (:require [core.utils.dom :as dom]
           [core.utils.dnd :as dnd]
           [core.tools :as t]
           [core.behaviours :as behaviours]
           [core.layouts :as layouts]
           [core.eventbus :as b]
           [core.events :as events]
           [core.rendering :as r]))

;; ====
;; move all global state for entities, event-bus, behaviours, components into this function as local mutable atoms.
;; ===

(defn initialize [id app-state config]
  (dom/console-log (str "Initializing relational-designer with id [ " id " ]."))
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
