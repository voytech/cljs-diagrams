(ns impl.renderers.default
  (:require [core.utils.general :refer [make-js-property]])

(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false
                           :lockRotation true
                           :lockScalingX true
                           :lockScalingY true})
(def LOCKED_MOVEMENT      {:lockMovementX true
                           :lockMovementY true})
(def NO_DEFAULT_CONTROLS {:hasControls false :hasBorders false})
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "black" :strokeWidth 1.5})
(def HANDLER_SMALLEST {:radius 8 :fill "#fff" :stroke "black" :strokeWidth 1.5})
(def CONNECTOR_DEFAULT_OPTIONS (merge DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS))


(defn- set [property]
  (fn [source value]
    (.set source (clj->js {property value}))))

(defn- fabric-properties [:left :top :width :height :x1 :x2 :y1 :y2 :border])

(defonce fabric-property-mapping {:left (set :left)
                                  :top  (set :top)
                                  :width (set :width)
                                  :height (set :height)})

(defn- property-change-render [drawable rendering-context]
  (let [source (:rendering-state drawable)
        propps   (-> rendering-context :redraw-properties)
        new    (:new rendering-context)]
      (doseq [prop props]
        (((:name prop) property-mapping) source (:new prop)))
      (.setCoords source)))

(defn- fabric-create-rendering-state [context drawable create]
  (let [fabric-object (create)]
     (make-js-property fabric-object "parentId"   (-> drawable :parent :id))
     (make-js-property fabric-object "parentType" (-> drawable :parent :type))
     (.add context fabric-object)
     fabric-object))

; in drawable-state we holds an fabric.js object.
; in context we have serveral properties. One of them is fabric.js canvas reference.
(defn- fabric-destroy-rendering-state [context state]
  (let [canvas (:canvas context)]
    (.remove canvas state)))

(defmethod do-render [:fabric :rect] [drawable context]
  (property-change-render drawable context))

(defmethod create-rendering-state [:fabric :rect] [drawable context]
  (fabric-create-rendering-state context drawable (fn[] (js/fabric.Rect. (clj->js (get-bbox drawable))))))

(defmethod do-render [:fabric :line] [drawable rendering-context]
  (property-change-render drawable rendering-context))

(defmethod create-rendering-state [:fabric :line] [drawable context]
  (fabric-create-rendering-state context drawable (fn [] (js/fabric.Line. (clj->js [(get-left drawable)
                                                                                    (get-top drawable)
                                                                                    (get-width drawable)
                                                                                    (get-height drawable)])))))

(defmethod do-render [:fabric :triangle] [drawable context]
  (property-change-render drawable context))

(defmethod create-rendering-state [:fabric :triangle] [drawable context]
  (fabric-create-rendering-state context drawable (fn [] (js/fabric.Triangle. (clj->js nil)))))
