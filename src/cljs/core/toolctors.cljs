(ns core.toolctors
 (:require [core.entities :as e])
 (:require-macros [core.macros :refer [defentity]]))

(def DEFAULT_SIZE_OPTS {:width 180 :height 150})
(def TRANSPARENT_FILL {:fill "rgb(255,255,255)"})
(def DEFAULT_STROKE {:stroke "#666" :strokeWidth 2})
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false :lockRotation true})
(def NO_DEFAULT_CONTROLS {:hasControls false :hasBorders false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "#666" :strokeWidth 2})
;; Below is an interface to js Fabric.js library.

(defn image [data options]
  (if (not (nil? options))
    (js/fabric.Image. data (clj->js options))
    (js/fabric.Image. data)))

(defn moving-connector-terminator [terminator-type pointX pointY])

(defn make-relationship? []
  (fn [e]
    (let [src    (:src e)
          entity (:entity e)
          part   (:part e)
          canvas (:canvas e)]
      (when (contains? #{"end" "start"} part)
        (.forEachObject canvas
                        #(when (and (not (== % src)) (contains? #{"connector-top" "connector-bottom"
                                                                  "connector-left" "connector-right" } (.-refPartId %)))
                           (when (.intersectsWithObject src %)
                             (let [trg %
                                   src-ent (e/entity-from-src src)
                                   trg-ent (e/entity-from-src trg)
                                   src-part (.-refPartId src)
                                   trg-part (.-refPartId trg)]
                                (e/connect-entities src-ent trg-ent (keyword part))))))))))

(defn moving-entity [part-name]
  (fn [e]
    (when (= (:part e) part-name)
      (let [entity (:entity e)
            event (:event e)
            movementX (.-movementX (.-e event))
            movementY (.-movementY (.-e event))]
        (doseq [part (:parts entity)]
          (when (not (= (:name part) part-name))
            (.set (:src part) (clj->js {:left (+ (.-left (:src part)) movementX)
                                        :top  (+ (.-top (:src part)) movementY)}))))
        (doseq [relation (:relationships entity)]
            (js/console.log relation)
            (let [end (:end relation)
                  related-entity (e/entity-by-id (:entity-id relation))
                  related-part   (e/entity-part related-entity (name end))
                  related-cl     (e/entity-part related-entity "connector")]
              (.set (:src related-part) (clj->js {:left (+ (.-left (:src related-part)) movementX)
                                                  :top  (+ (.-top (:src related-part)) movementY)}))
              (.set (:src related-cl)
                (if (= :start end)
                  (clj->js {:x1 (+ (.-x1 (:src related-cl)) movementX)
                            :y1  (+ (.-y1 (:src related-cl)) movementY)})
                  (clj->js {:x2 (+ (.-x2 (:src related-cl)) movementX)
                            :y2  (+ (.-y2 (:src related-cl)) movementY)})))))))))

(defn moving-connector [coordX coordY]
   (fn [e]
      (let [src (:src e)
            entity (:entity e)
            connector (e/entity-part entity "connector")]
         (.set (:src connector) (clj->js {(keyword coordX) (+ (.-left src) 8)
                                          (keyword coordY) (+ (.-top src) 8)})))))




(e/handle-event "object:moving" (moving-connector "start" "x1" "y1"))
(e/handle-event "object:moving" (moving-connector "end" "x2" "y2"))
(e/handle-event "object:moving" (moving-entity "body"))
(e/handle-event "mouse:up" (make-relationship?))

(defmulti connector (fn [point & {:keys [moveable display]}] display))

(defmethod connector "circle" [point & {:keys [moveable display]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))}
                       HANDLER_SMALL
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Circle. (clj->js options))))

(defmethod connector "rect" [point & {:keys [moveable display]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))
                        :width (* 2 (:radius HANDLER_SMALL))
                        :height (* 2 (:radius HANDLER_SMALL))}
                       (dissoc HANDLER_SMALL :radius)
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Rect. (clj->js options))))


(defentity rect data options
  (with-drawables
    (let [enriched-opts (merge options
                               DEFAULT_SIZE_OPTS
                               TRANSPARENT_FILL
                               DEFAULT_STROKE
                               RESTRICTED_BEHAVIOUR
                               NO_DEFAULT_CONTROLS)
          conL    (vector (:left options) (+ (/ (:height DEFAULT_SIZE_OPTS) 2) (:top options)))
          conR    (vector (+ (:left options) (:width DEFAULT_SIZE_OPTS)) (+ (/ (:height DEFAULT_SIZE_OPTS) 2) (:top options)))
          conT    (vector (+ (/ (:width DEFAULT_SIZE_OPTS) 2) (:left options)) (:top options))
          conB    (vector (+ (/ (:width DEFAULT_SIZE_OPTS) 2) (:left options)) (+ (:top options) (:height DEFAULT_SIZE_OPTS)))]
      ["connector-left"   (connector conL :moveable false :display "rect")
       "connector-right"  (connector conR :moveable false :display "rect")
       "connector-top"    (connector conT :moveable false :display "rect")
       "connector-bottom" (connector conB :moveable false :display "rect")
       "body"             (js/fabric.Rect. (clj->js enriched-opts))]))
  (with-behaviours
    ["body" "object:moving" (moving-entity "body")]))


(defentity connector-line data options
  (with-drawables
    (let [enriched-opts (merge options DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS)
          offset-x (:left options)
          offset-y (:top options)
          points-pairs (partition 2 data)
          points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
          conS (first points-pairs-offset)
          conE (last points-pairs-offset)]
        ["connector" (js/fabric.Line. (clj->js (flatten points-pairs-offset)) (clj->js enriched-opts))
         "start"     (connector conS :moveable true :display "circle")
         "end"       (connector conE :moveable true :display "circle")]))
  (with-behaviours
    ["start" "object:moving"  (moving-connector "x1" "y1")
     "start" "mouse:up"       (make-relationship?)
     "end" "object:moving"    (moving-connector "x2" "y2")
     "end" "mouse:up"         (make-relationship?)]))

(defn circle [options])
(defn triangle [options])
(defn ellipse [options])
(defn polyline [options])
(defn polygon [options])
(defn group [])
(defn text [data options])
(defn path [])

(defn create
  ([entity data]
   (fn [context]
     (entity data context)))
  ([entity]
   (fn [context]
     (entity nil context))))
