(ns cljs-diagrams.impl.renderers.default
  (:require [cljs-diagrams.core.utils.general :refer [make-js-property]]
            [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.rendering :as r]
            [cljs-diagrams.impl.components :as impld]))

;Some fabric object specific options to pass.
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false
                           :lockRotation true
                           :lockScalingX true
                           :lockScalingY true})
(def LOCKED_SCALING       {:lockScalingX true
                           :lockScalingY true})
(def LOCKED_ROTATION      {:lockRotation true})
(def LOCKED_MOVEMENT      {:lockMovementX true
                           :lockMovementY true})
(def NO_DEFAULT_CONTROLS  {:hasControls false
                           :hasBorders false
                           :hasRotatingPoint false})

(def LOCKED (merge LOCKED_MOVEMENT LOCKED_ROTATION LOCKED_SCALING NO_DEFAULT_CONTROLS))

(defn- fabric-set
  ([source property value]
   (.set source property value))
  ([source map_]
   (.set source (clj->js map_))))


(defonce fabric-property-mapping {:left "left"
                                  :top  "top"
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

(defonce fabric-value-mapping {:top 100000
                               :bottom 0})

(defn- resolve-value [val]
  (if (keyword? val)
    (or (val fabric-value-mapping) val)
    val))

(defn- to-fabric-property-map [input-map]
  (apply merge (mapv (fn [e] {(keyword (or (e fabric-property-mapping) e)) (resolve-value (e input-map))}) (keys input-map))))

(defn- fabric-apply [drawable source properties]
  (doseq [p properties]
    (fabric-set source (or (p fabric-property-mapping) p) (resolve-value (d/getp drawable p)))))

(defn- synchronize-dimmensions [drawable]
  (let [source (:data (r/get-state-of drawable))]
    (when (or (nil? (d/get-width drawable))
              (= :draw-text (:rendering-method drawable))) (d/set-width  drawable (.-width  source)))
    (when (or (nil? (d/get-height drawable))
              (= :draw-text (:rendering-method drawable))) (d/set-height drawable (.-height source)))))

(defn- property-change-render [drawable rendering-context]
  (let [source  (:data (r/get-state-of drawable))
        redraw   (get-in rendering-context [:redraw-properties (:uid drawable)])]
      (fabric-apply drawable source redraw)
      (synchronize-dimmensions drawable)))

(defn- fabric-create-rendering-state [context drawable create]
  (let [fabric-object (create)]
     (make-js-property fabric-object "refId" (:uid drawable))
     (.add (:canvas context) fabric-object)
     (.moveTo (get context :canvas) fabric-object (resolve-value (d/getp drawable :z-index)))
     (.renderAll (get context :canvas))
     {:data fabric-object}))

; in drawable-state we holds an fabric.js object.
; in context we have serveral properties. One of them is fabric.js canvas reference.
(defn- fabric-destroy-rendering-state [context state]
  (let [canvas (:canvas context)]
    (.remove canvas (:data state))))

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :fabric [dom-id width height]
  (let [canvas (js/fabric.StaticCanvas. dom-id)]
     (.setWidth canvas width)
     (.setHeight canvas height)
     canvas))

(defmethod r/all-rendered :fabric [context]
  (.renderAll (get context :canvas)))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-rect] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :draw-rect] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Rect. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-rect] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; startpoint rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-circle] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :draw-circle] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-circle] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))


;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-line] [drawable rendering-context]
  (property-change-render drawable rendering-context))

(defmethod r/create-rendering-state [:fabric :draw-line] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Line. (clj->js [(:x1 data) (:y1 data) (:x2 data) (:y2 data)]) (clj->js data))))))


(defmethod r/destroy-rendering-state [:fabric :draw-line] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-triangle] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :draw-triangle] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Triangle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-triangle] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-text] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :draw-text] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Text. (:text data) (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-text] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))
