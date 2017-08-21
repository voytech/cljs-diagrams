(ns impl.renderers.default
  (:require [core.utils.general :refer [make-js-property]])
  (:require [core.drawables :as d])
  (:require [core.rendering :as r]))

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
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "black" :strokeWidth 1.5})
(def HANDLER_SMALLEST {:radius 8 :fill "#fff" :stroke "black" :strokeWidth 1.5})
(def CONNECTOR_DEFAULT_OPTIONS (merge DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS))

(defn- set
  ([source property value
    (.set source (clj->js {property value}))])
  ([source map
    (.set source (clj->js map))]))


(defonce fabric-property-mapping {:left :left
                                  :top  :top
                                  :width  :width
                                  :height :height
                                  :origin-x :originX
                                  :origin-y :originY
                                  :angle :angle
                                  :x1 :x1
                                  :y1 :y1
                                  :x2 :x2
                                  :y2 :y2
                                  :border-color  :stroke
                                  :background-color :fill
                                  :radius :radius
                                  :font-family :fontFamily
                                  :font-weight :fontWeight
                                  :border-width :strokeWidth})

(defn to-fabric-property-map [input-map]
  (apply merge (map (fn [e] {(e farbric-property-map) (e input-map)}) (keys input-map))))

(defn- property-change-render [drawable rendering-context]
  (let [source  (:rendering-state drawable)
        props   (-> rendering-context :redraw-properties)
        new     (:new rendering-context)]
      (doseq [prop props]
        (set source (prop fabric-property-mapping) (:new prop)))
      (.setCoords source)
      (r/update-context {:redraw-properties nil})))

(defn- fabric-create-rendering-state [context drawable create]
  (let [fabric-object (create)]
     (make-js-property fabric-object "refId"  (:uid drawable))
     (.add (:canvas context) fabric-object)
     fabric-object))

; in drawable-state we holds an fabric.js object.
; in context we have serveral properties. One of them is fabric.js canvas reference.
(defn- fabric-destroy-rendering-state [context state]
  (let [canvas (:canvas context)]
    (.remove canvas state)))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :rect] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :rect] [drawable context]
  (let [data (d/state drawable)]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Rect. (clj->js data))))))

(defmethod r/destroy-rendering-state [state context]
  (fabric-destroy-rendering-state context state))

;;==========================================================================================================
;; circle rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :circle] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :circle] [drawable context]
  (let [data (d/state drawable)]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [state context]
  (fabric-destroy-rendering-state context state))

;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :line] [drawable rendering-context]
  (property-change-render drawable rendering-context))

(defmethod r/create-rendering-state [:fabric :line] [drawable context]
  (let [data (to-fabric-property-map (state drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Line. (clj->js [(d/get-left drawable)
                                                                                      (d/get-top drawable)
                                                                                      (d/get-width drawable)
                                                                                      (d/get-height drawable)]))))))

(defmethod r/destroy-rendering-state [state context]
  (fabric-destroy-rendering-state context state))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :triangle] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :triangle] [drawable context]
  (let [data (to-fabric-property-map (d/state drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Triangle. (clj->js data))))))

(defmethod r/destroy-rendering-state [state context]
  (fabric-destroy-rendering-state context state))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric :triangle] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric :triangle] [drawable context]
  (let [data (to-fabric-property-map (d/state drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Text. (:text data) (clj->js data))))))

(defmethod r/destroy-rendering-state [state context]
  (fabric-destroy-rendering-state context state))
