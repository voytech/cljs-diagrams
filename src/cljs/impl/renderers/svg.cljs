(ns impl.renderers.svg
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]
            [core.rendering :as r]
            [core.utils.dom :as dom]
            [impl.components :as impld]))

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :svg [dom-id width height]
  (let [svg-container (dom/by-id dom-id)]
     svg-container))

(defmethod r/all-rendered :svg [context]
  (console.log "all-rendered: SVG renderer does not support this type of method."))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-rect] [drawable context]
  (console.log "do-render :draw-rect has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-rect] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Rect. (clj->js data))))))

(defmethod r/destroy-rendering-state [:svg :draw-rect] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))


;;==========================================================================================================
;; startpoint rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-circle] [drawable context]
  (console.log "do-render :draw-rect has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-circle] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:svg :draw-circle] [drawable context]
  (console.log "destroy-rendering-state :draw-rect has been not yet implemented."))


;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-line] [drawable rendering-context]
  (console.log "do-render :draw-line has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-line] [drawable context]
  (console.log "create-rendering-state :draw-line has been not yet implemented."))

(defmethod r/destroy-rendering-state [:svg :draw-line] [drawable context]
  (console.log "destroy-rendering-state :draw-line has been not yet implemented."))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-triangle] [drawable context]
  (console.log "do-render :draw-triangle has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-triangle] [drawable context]
  (console.log "create-rendering-state :draw-triangle has been not yet implemented."))

(defmethod r/destroy-rendering-state [:svg :draw-triangle] [drawable context]
  (console.log "destroy-rendering-state :draw-triangle has been not yet implemented."))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-text] [drawable context]
  (console.log "do-render :draw-text has been not yet implemented."))

(defmethod r/create-rendering-state [:svg :draw-text] [drawable context]
  (console.log "create-rendering-state :draw-text has been not yet implemented."))

(defmethod r/destroy-rendering-state [:svg :draw-text] [drawable context]
  (console.log "destroy-rendering-state :draw-text has been not yet implemented."))
