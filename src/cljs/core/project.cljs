(ns core.project
 (:require [reagent.core :as reagent :refer [atom]]
           [cljsjs.jquery]
           [cljsjs.fabric]
           [core.utils.dom :as dom]
           [core.utils.dnd :as dnd]
           [core.entities :as e]
           [core.tools :as t]
           [core.eventbus :as b]
           [core.rendering :as r]
           [impl.renderers.default :as dd]
           [core.layouts :as layouts]))

(defonce project (atom {}))

(defonce event-map {"object:moving" "moving"
                    "mouse:down" "mousedown"
                    "mouse:up" "mouseup"
                    "mouse:click" "mouseclick"
                    "mouse:dbclick" "mousedbclick"
                    "mouse:over" "mouseover"
                    "mouse:out" "mouseout"})

(defn- normalise-event [event]
  (get event-map event))

(defn- decompose [event event-type]
  (let [drawable-id        (.-refId (.-target event))
        entity             (e/lookup drawable-id :entity)
        component          (e/lookup drawable-id :component)
        attribute-value    (e/lookup drawable-id :attribute)
        drawable           (:drawable component)]
    {:entity           entity
     :attribute-value  attribute-value
     :drawable         drawable
     :component        component
     :event event
     :x (.-layerX event)
     :y (.-layerY event)
     :movement-x (.-movementX event)
     :movement-y (.-movementY event)
     :type       event-type}))

(defn- dispatch-events [canvas]
  (doseq [event-type (keys event-map)]
      (.on canvas (js-obj event-type (fn [e]
                                       (when (not (nil? (.-target e)))
                                         (let [normalised-event-type (normalise-event event-type)
                                               decomposed (decompose e normalised-event-type)
                                               type-to-name (fn [decomposed class-kwrd] (name (-> decomposed class-kwrd :type)))]
                                           (b/fire (str (type-to-name decomposed :entity) "." (type-to-name decomposed :component) "." normalised-event-type) decomposed))))))))

(defn initialize [id {:keys [width height]}]
  (dom/console-log (str "Initializing canvas with id [ " id " ]."))
  (let [data {:canvas (js/fabric.Canvas. id)
              :id id
              :width width
              :height height}]
    (.setWidth (:canvas data) width)
    (.setHeight (:canvas data) height)
    (reset! project data)
    (dispatch-events (:canvas data))
    (b/fire "rendering.context.update" {:canvas (:canvas data)})))
  ;;(let [canvas (:canvas (proj-page-by-id id))]
  ;;  (do (.setWidth canvas @zoom-page-width)
  ;;      (.setHeight canvas @zoom-page-height)
  ;;  (.setZoom canvas @zoom))

;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

;TODO how should we handle dragNdrop events originating from particulaar tool? If not all tools produces entities - some can have different behaviour in canvas context
; For example : attribute value producing tool will bind attrib value to entity. It in fact can just return entity to which attribute value was added
; then this entity is going to be synchronized - all changes made are going to be propageted to canvas.

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)]
    (t/invoke-tool tool-obj context)))
