(ns impl.renderers.svg
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]
            [core.rendering :as r]
            [core.utils.dom :as dom]
            [core.utils.svg :as svg]
            [impl.components :as impld]))

(defonce svg-property-mapping {:left "x"
                               :top  "y"
                               :round-x "rx"
                               :round-y "ry"
                               :text "text"
                               :width  "width"
                               :height "height"
                               :origin-x "originX"
                               :origin-y "originY"
                               :angle "angle"
                               :x1 "x1"
                               :y1 "y1"
                               :x2 "x2"
                               :y2 "y2"
                               :z-index "zIndex"
                               :border-color  "stroke"
                               :background-color "fill"
                               :radius "radius"
                               :font-family "fontFamily"
                               :font-weight "fontWeight"
                               :font-size "fontSize"
                               :text-align "textAlign"
                               :visible "visible"
                               :color "color"
                               :border-width "strokeWidth"})

(defn- svg-shape-attributes [component-model]

  )

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :svg [dom-id width height]
  (svg/create-svg dom-id "svg" {:width width :height height}))

(defmethod r/all-rendered :svg [context]
  (console.log "all-rendered: SVG renderer does not support this type of method."))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-rect] [component context]
  (console.log "do-render :draw-rect has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-rect] [component context]
  (let [attributes (svg-shape-attributes (d/model component))]
     (svg/create-rect (:canvas context) attributes)))

(defmethod r/destroy-rendering-state [:svg :draw-rect] [component context]
  (console.log "destroy-rendering-state :draw-rect has been not yet implemented."))

;;==========================================================================================================
;; startpoint rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-circle] [component context]
  (console.log "do-render :draw-rect has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-circle] [component context]
  (console.log "create-rendering-state :draw-circle has been not yet implemented."))

(defmethod r/destroy-rendering-state [:svg :draw-circle] [component context]
  (console.log "destroy-rendering-state :draw-rect has been not yet implemented."))


;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-line] [component rendering-context]
  (console.log "do-render :draw-line has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-line] [component context]
  (console.log "create-rendering-state :draw-line has been not yet implemented."))

(defmethod r/destroy-rendering-state [:svg :draw-line] [component context]
  (console.log "destroy-rendering-state :draw-line has been not yet implemented."))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-triangle] [component context]
  (console.log "do-render :draw-triangle has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-triangle] [component context]
  (console.log "create-rendering-state :draw-triangle has been not yet implemented."))

(defmethod r/destroy-rendering-state [:svg :draw-triangle] [component context]
  (console.log "destroy-rendering-state :draw-triangle has been not yet implemented."))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-text] [component context]
  (console.log "do-render :draw-text has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-text] [component context]
  (console.log "create-rendering-state :draw-text has been not yet implemented."))

(defmethod r/destroy-rendering-state [:svg :draw-text] [component context]
  (console.log "destroy-rendering-state :draw-text has been not yet implemented."))
