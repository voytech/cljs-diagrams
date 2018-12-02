(ns core.project
 (:require [core.utils.dom :as dom]
           [core.utils.dnd :as dnd]
           [core.tools :as t]
           [core.eventbus :as b]
           [core.events :as events]
           [core.rendering :as r]))

(defonce project (atom {}))

(defonce project-state (volatile! {}))

(defn get-container []
  (aget (dom/parent (dom/j-query-id (-> @project :id))) 0))

(defn make-selection [targets]
  (vswap! project-state assoc :selection targets))

(defn get-selection []
  (get @project-state :selection))

(defn append-state [key value]
  (vswap! project-state assoc key value))

(defn get-state [key]
  (get @project-state key))

(defn remove-state [key]
  (vswap! project-state dissoc key))

(defn clear-selection []
  (vswap! project-state dissoc :selection))
;; ====
;; move all global state for entities, event-bus, behaviours, components into this function as local mutable atoms.
;; ===

(defn initialize [id {:keys [width height renderer] :as config}]
  (dom/console-log (str "Initializing relational-designer with id [ " id " ]."))
  (let [app-state (atom {:dom {:id id :width width :height height}
                         :events (:events config)
                         :entities (atom {})})]
    (console.log "initializing event-bus ...")
    (b/initialize app-state)                       
    (console.log "Initializing renderer ...")
    (r/create-renderer app-state id width height renderer)
    (console.log "dispatching events ...")
    (events/dispatch-events app-state)
    @app-state))
;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event state]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)]
    (t/invoke-tool tool-obj (-> state deref :entities) context)))
