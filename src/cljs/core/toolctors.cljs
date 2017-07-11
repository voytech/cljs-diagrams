(ns core.toolctors
 (:require [core.entities :as e]))

(def DEFAULT_SIZE_OPTS {:width 100 :height 100})
(def TRANSPARENT_FILL {:fill "rgba(0,0,0,0)"})
(def DEFAULT_STROKE {:stroke "#666" :strokeWidth 2})
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false :lockRotation true})
(def NO_DEFAULT_CONTROLS {:hasControls false :hasBorders false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "#666" :strokeWidth 2})
;; Below is an interface to js Fabric.js library.

(defn image [data options]
  (if (not (nil? options))
    (js/fabric.Image. data (clj->js options))
    (js/fabric.Image. data)))

(defn rect [options]
  (let [enriched-opts (merge options
                             DEFAULT_SIZE_OPTS
                             TRANSPARENT_FILL
                             DEFAULT_STROKE
                             RESTRICTED_BEHAVIOUR
                             NO_DEFAULT_CONTROLS)]
    [(e/Part. "body" (js/fabric.Rect. (clj->js enriched-opts)) {})]))

(defn handle [point]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point) (:radius HANDLER_SMALL))}
                       HANDLER_SMALL
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Circle. (clj->js options))))


(defn connector [points options]
  (let [enriched-opts (merge options DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS)
        offset-x (:left options)
        offset-y (:top options)
        points-pairs (partition 2 points)
        points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
        move-connector (fn [handle-name coordX coordY]
                         (fn [e]
                           (when (= (:part e) handle-name)
                             (js/console.log (:entity e))
                             (let [src (:src e)
                                   entity (:entity e)
                                   connector (e/entity-part entity "connector")]
                                (.set (:src connector) (clj->js {(keyword coordX) (+ (.-left src) 8)
                                                                 (keyword coordY) (+ (.-top src) 8)}))))))]
    [(e/Part. "connector"
              (js/fabric.Line. (clj->js (flatten points-pairs-offset)) (clj->js enriched-opts))
              {"object:moving" (fn [e])})
     (e/Part. "start"
              (handle (first points-pairs-offset))
              {"object:moving" (move-connector "start" "x1" "y1")})
     (e/Part. "end"
              (handle (last points-pairs-offset))
              {"object:moving" (move-connector "end" "x2" "y2")})]))

(defn circle [options])
(defn triangle [options])
(defn ellipse [options])
(defn polyline [options])
(defn polygon [options])
(defn group [])
(defn text [data options])
(defn path [])

(defn create
  ([parts data]
   (fn [context]
     (e/create-entity "" (parts data context))))
  ([parts]
   (fn [context]
     (e/create-entity "" (parts context)))))
