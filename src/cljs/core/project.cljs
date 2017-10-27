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
           [impl.renderers.default :as dd]
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

(defonce source-events "click dbclick mousemove mousedown mouseup mouseenter mouseleave keypress keydown keyup")

(defn initialize [id {:keys [width height]}]
  (dom/console-log (str "Initializing canvas with id [ " id " ]."))
  (let [data {:canvas (js/fabric.StaticCanvas. id)
              :id id
              :width width
              :height height}]
    (.setWidth (:canvas data) width)
    (.setHeight (:canvas data) height)
    (reset! project data)
    (events/dispatch-events id (clojure.string/split source-events #" "))
    (b/fire "rendering.context.update" {:canvas (:canvas data)})))

;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)]
    (t/invoke-tool tool-obj context)))
