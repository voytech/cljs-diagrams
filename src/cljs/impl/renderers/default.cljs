(ns impl.renderers.default
  (:require [core.utils.general :refer [make-js-property]]
            [core.drawables :as d]
            [core.eventbus :as b]
            [core.rendering :as r]))

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

(defn- to-fabric-property-map [input-map]
  (apply merge (cons LOCKED (mapv (fn [e] {(keyword (or (e fabric-property-mapping) e)) (e input-map)}) (keys input-map)))))

(defn- fabric-apply [drawable source properties]
  (doseq [p properties]
    (fabric-set source (or (p fabric-property-mapping) p) (d/getp drawable p))))

(defn- synchronize-bounds [drawable]
  (let [source (:data (d/state drawable))]
    (d/set-width  drawable (.-width  source))
    (d/set-height drawable (.-height source))))

(defn- property-change-render [drawable rendering-context]
  (let [source  (:data (d/state drawable))
        redraw   (get-in rendering-context [:redraw-properties (:uid drawable)])]
      (fabric-apply drawable source redraw)
      (synchronize-bounds drawable)))

;(b/on ["rendering.finish"] -999 (fn [e] (.renderAll (get @r/rendering-context :canvas))))

(defn- fabric-create-rendering-state [context drawable create]
  (let [fabric-object (create)]
     (make-js-property fabric-object "refId" (:uid drawable))
     (.add (:canvas context) fabric-object)
     {:data fabric-object}))

; in drawable-state we holds an fabric.js object.
; in context we have serveral properties. One of them is fabric.js canvas reference.
(defn- fabric-destroy-rendering-state [context state]
  (let [canvas (:canvas context)]
    (.remove canvas (:data state))))

(defmethod r/all-rendered :fabric [context]
  (.renderAll (get context :canvas)))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :rect] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :rect] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Rect. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :rect] [drawable context]
  (fabric-destroy-rendering-state context (d/state drawable)))

;;==========================================================================================================
;; circle rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :circle] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :circle] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :circle] [drawable context]
  (fabric-destroy-rendering-state context (d/state drawable)))

;;==========================================================================================================
;; line rendering (must be improved ... changes to x1 x2 y1 y2 on static canvas not working as expected)
;; As a temporary workaround - we are removing rendering state of drawable and creating new line as a new rendering state with new
;; x1 x2 y1 y2 properties. Also take into account that synchronization of x1 x2 y1 y2 with left top widht height must be better.
;;==========================================================================================================
(defmethod r/do-render [:fabric :line] [drawable rendering-context]
  (fabric-destroy-rendering-state rendering-context (d/state drawable))
  (let [data (dissoc (to-fabric-property-map (d/model drawable)) :width :height :left :top)
        state (fabric-create-rendering-state rendering-context drawable (fn [] (js/fabric.Line. (clj->js [(:x1 data) (:y1 data) (:x2 data) (:y2 data)]) (clj->js data))))]
    (.moveTo (:data state) (get data "zIndex"))
    (d/update-state drawable state)))
  ;(property-change-render drawable rendering-context))

(defmethod r/create-rendering-state [:fabric :line] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Line. (clj->js [(:x1 data) (:y1 data) (:x2 data) (:y2 data)]) (clj->js data))))))


(defmethod r/destroy-rendering-state [:fabric :line] [drawable context]
  (fabric-destroy-rendering-state context (d/state drawable)))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :triangle] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :triangle] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Triangle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :triangle] [drawable context]
  (fabric-destroy-rendering-state context (d/state drawable)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :text] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :text] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Text. (:text data) (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric :text] [drawable context]
  (fabric-destroy-rendering-state context (d/state drawable)))
