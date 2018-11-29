(ns core.project
 (:require [cljsjs.jquery]
           [cljsjs.fabric]
           [cljsjs.rx]
           [core.utils.dom :as dom]
           [core.utils.dnd :as dnd]
           [core.entities :as e]
           [core.tools :as t]
           [core.eventbus :as b]
           [core.events :as events]
           [core.rendering :as r]
           [core.components :as d]
           [core.layouts :as layouts]))

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
;; state = {
;;  :renderer {},
;;  :events {},
;;  :entities  {},
;;  :behaviours {} }
;; }
(defn initialize [id {:keys [width height renderer] :as config}]
  (dom/console-log (str "Initializing relational-designer with id [ " id " ]."))
  (let [data {:canvas (r/create id width height renderer)
              :id id
              :width width
              :height height}
        app-state (atom {:dom {:id id :width width :height height}
                         :renderer {},
                         :events (:events config),
                         :entities (atom {}),
                         :behaviours {}})]
    (reset! project data)
    (events/dispatch-events app-state)))
;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)]
    (t/invoke-tool tool-obj context)))
