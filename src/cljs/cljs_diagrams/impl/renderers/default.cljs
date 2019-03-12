(ns cljs-diagrams.impl.renderers.default
  (:require [cljs-diagrams.core.utils.general :refer [make-js-property]]
            [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.rendering :as r]))

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

(defn- fabric-apply [component source properties]
  (doseq [p properties]
    (fabric-set source (or (p fabric-property-mapping) p) (resolve-value (d/getp component p)))))

(defn- synchronize-dimmensions [source component]
  (when (or (nil? (d/get-width component))
            (= :draw-text (:rendering-method component))) (d/set-width  component (.-width  source)))
  (when (or (nil? (d/get-height component))
            (= :draw-text (:rendering-method component))) (d/set-height component (.-height source))))

(defn- property-change-render [rendering-state component properties]
  (let [source   (get-in rendering-state [:components (:uid component) :handle])]
      (fabric-apply component source properties)
      (synchronize-dimmensions source component)))

(defn- fabric-create-rendering-state [rendering-state component create]
  (let [fabric-object (create)]
     (make-js-property fabric-object "refId" (:uid component))
     (.add (:canvas rendering-state) fabric-object)
     (.moveTo (get rendering-state :canvas) fabric-object (resolve-value (d/getp component :z-index)))
     (.renderAll (get rendering-state :canvas))
     fabric-object))

; in drawable-state we holds an fabric.js object.
; in context we have serveral properties. One of them is fabric.js canvas reference.
(defn- fabric-destroy-rendering-state [rendering-state component]
  (let [canvas (:canvas rendering-state)
        source (get-in rendering-state [:components (:uid component) :handle])]
    (.remove canvas source)))

(defmethod r/is-state-created :fabric [renderer-state component]
  (some? (get-in renderer-state [:components (:uid component) :handle])))

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :fabric [renderer dom-id width height]
  (let [canvas (js/fabric.StaticCanvas. (str dom-id "-canvas"))]
     (.setWidth canvas width)
     (.setHeight canvas height)
    {:canvas canvas}))

(defmethod r/all-rendered :fabric [state context]
  (.renderAll (get context :canvas)))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-rect] [renderer-state component properties]
  (property-change-render renderer-state component properties))

(defmethod r/create-rendering-state [:fabric :draw-rect] [renderer-state component]
  (let [data (to-fabric-property-map (d/model component))]
    (fabric-create-rendering-state renderer-state component (fn [] (js/fabric.Rect. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-rect] [renderer-state component]
  (fabric-destroy-rendering-state renderer-state component))

;;==========================================================================================================
;; circle rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-circle] [renderer-state component properties]
  (property-change-render renderer-state component properties))

(defmethod r/create-rendering-state [:fabric :draw-circle] [renderer-state component]
  (let [data (to-fabric-property-map (d/model component))]
    (fabric-create-rendering-state renderer-state component (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-circle] [renderer-state component]
  (fabric-destroy-rendering-state renderer-state component))


;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-line] [renderer-state component properties]
  (property-change-render renderer-state component properties))

(defmethod r/create-rendering-state [:fabric :draw-line] [renderer-state component]
  (let [data (to-fabric-property-map (d/model component))]
    (fabric-create-rendering-state renderer-state component (fn [] (js/fabric.Line. (clj->js [(:x1 data) (:y1 data) (:x2 data) (:y2 data)]) (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-line] [renderer-state component]
  (fabric-destroy-rendering-state renderer-state component))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :draw-text] [renderer-state component properties]
  (property-change-render renderer-state component properties))


(defmethod r/create-rendering-state [:fabric :draw-text] [renderer-state component]
  (let [data (to-fabric-property-map (d/model component))]
    (fabric-create-rendering-state renderer-state  component (fn [] (js/fabric.Text. (:text data) (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :draw-text] [renderer-state component]
  (fabric-destroy-rendering-state renderer-state component))
