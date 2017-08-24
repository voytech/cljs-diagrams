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
           [core.drawables :as d]
           [impl.renderers.default :as dd]
           [core.layouts :as layouts]))

(defonce project (atom {}))

(defonce bucket-size 50)

(defonce drawable-buckets (atom {}))

(defn- update-buckets [drawable]
  (doseq [key (get @drawable-buckets (:uid drawable))]
    (swap! drawable-buckets dissoc key))
  (swap! drawable-buckets dissoc (:uid drawable))
  (let [x-s (js/Math.floor (/ (:left drawable) bucket-size))
        y-s (js/Math.floor (/ (:top drawable) bucket-size))
        x-e (+ bucket-size (js/Math.floor (/ (+ (:left drawable) (:width drawable)) bucket-size)))
        y-e (+ bucket-size (js/Math.floor (/ (+ (:top drawable) (:height drawable)) bucket-size)))]
    (doseq [x (range x-s x-e)
            y (range y-s y-e)]
      (let [coord-key (str x "." y)
            drawables (or (get @drawable-buckets coord-key) [])
            keys      (or (get @drawable-buckets (:uid drawable)) [])]
        (swap! drawable-buckets assoc coord-key (cons (:uid drawable) drawables))
        (swap! drawable-buckets assoc (:uid drawable) (cons coord-key keys))))))

;(b/on ["drawable.created" "drawable.changed"] -999 (fn [e] (update-buckets (-> e :context :drawable))))
(defn- lookup [x y]
  (first (filter (fn [e]
                   (and (>= x (d/get-left e)) (<= x (+ (d/get-left e) (d/get-width e)))
                        (>= y (d/get-top e)) (<= y (+ (d/get-top e) (d/get-height e))))) (vals @e/drawables))))


(defn- lookup-drawable [x y]
  (let [x-s (js/Math.floor (/ x bucket-size))
        y-s (js/Math.floor (/ y bucket-size))]
    (doseq [drawable (get @drawable-buckets (str x-s "." y-s))]
      (when (and (>= x (d/get-left drawable)) (<= x (+ (d/get-left drawable) (d/get-width drawable)))
                 (>= y (d/get-top drawable)) (<= y (+ (d/get-top drawable) (d/get-height drawable))))
          drawable))))

(defonce event-map {"object:moving" "moving"
                    "mousedown" "mousedown"
                    "mouseup" "mouseup"
                    "click" "mouseclick"
                    "dbclick" "mousedbclick"
                    "mousemove" "mousemove"
                    "mouseenter" "mouseenter"
                    "mouseleave" "mouseleave"})

(defonce source-events "click dbclick mousemove mousedown mouseup mouseenter mouseleave keypress keydown keyup")

(defn- normalise-event [event]
  (get event-map event))

(defn- decompose [x y event event-type drawable-id]
  (let [entity             (e/lookup drawable-id :entity)
        component          (e/lookup drawable-id :component)
        attribute-value    (e/lookup drawable-id :attribute)
        drawable           (:drawable component)]
    {:entity           entity
     :attribute-value  attribute-value
     :drawable         drawable
     :component        component
     :event event
     :x x
     :y y
     ;:movement-x (.-movementX event)
     ;:movement-y (.-movementY event)
     :type       event-type}))

(defn- event-name [decomposed]
   (let [entity-type    (str (name (-> decomposed :entity :type)) ".")
         attribute-type (if (not (nil? (:attribute-value decomposed)))
                            (str (name (-> decomposed :attribute-value :attribute :name)) ".")
                            "")
         component-type (str (name (-> decomposed :component :type)) ".")]
      (str entity-type attribute-type component-type (:type decomposed))))

(defn- bind-dispatch-listener [id]
  (let [obj  (js/document.getElementById id)]
    (.bind (js/jQuery (str "#" id)) source-events (fn [e]
                                                    (let [rect (.getBoundingClientRect obj)
                                                          cx (- (.-clientX e) (.-left rect))
                                                          cy (- (.-clientY e) (.-top rect))
                                                          event-type (normalise-event (.-type e))
                                                          decomposed (decompose cx cy e event-type (:uid (lookup cx cy)))]
                                                       (when-not (nil? (:entity decomposed))
                                                        (js/console.log (str "on " (event-name decomposed)))
                                                        (b/fire (event-name decomposed) decomposed)))))))

(defn initialize [id {:keys [width height]}]
  (dom/console-log (str "Initializing canvas with id [ " id " ]."))
  (let [data {:canvas (js/fabric.StaticCanvas. id)
              :id id
              :width width
              :height height}]
    (.setWidth (:canvas data) width)
    (.setHeight (:canvas data) height)
    (reset! project data)
    (bind-dispatch-listener id)
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
