(ns core.toolctors
 (:require [core.entities :as e]
           [clojure.string :as str])
 (:require-macros [core.macros :refer [defentity]]))

(declare position-endpoint)
(declare calculate-angle)

(def DEFAULT_SIZE_OPTS {:width 180 :height 150})
(def TRANSPARENT_FILL {:fill "rgb(255,255,255)"})
(def DEFAULT_FILL {:fill "#666"})
(def DEFAULT_STROKE {:stroke "#666" :strokeWidth 1.5})
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false :lockRotation true :lockScalingX true :lockScalingY true})
(def NO_DEFAULT_CONTROLS {:hasControls false :hasBorders false})
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "#666" :strokeWidth 1.5})
(def HANDLER_SMALLEST {:radius 8 :fill "#fff" :stroke "#666" :strokeWidth 1.5})
(def DEFAULT_OPTIONS {:highlight-color "red"
                      :normal-color "#666"
                      :highlight-width 3
                      :normal-width 1.5})
;; Below is an interface to js Fabric.js library.

(defn image [data options]
  (if (not (nil? options))
    (js/fabric.Image. data (clj->js options))
    (js/fabric.Image. data)))

(defn highlight [bln options]
  (fn [e]
    (.set (:src e) (clj->js {:stroke (if bln (:highlight-color options)
                                             (:normal-color options))
                             :strokeWidth (if bln (:highlight-width options)
                                                  (:normal-width options))}))))

(defn overlaying? [src trg]
    (or (.intersectsWithObject src trg)
        (.isContainedWithinObject src trg)
        (.isContainedWithinObject trg src)))

(defn intersects-any? [parts yes]
  (fn [e]
    (let [src    (:src e)
          entity (:entity e)
          part   (:part e)
          canvas (:canvas e)]
      (when (contains? #{"end" "start"} part)
          (.forEachObject canvas
                          #(when (and (not (== % src)) (contains? parts (.-refPartId %)))
                             (let [trg %
                                   src-ent (e/entity-from-src src)
                                   trg-ent (e/entity-from-src trg)
                                   src-part (.-refPartId src)
                                   trg-part (.-refPartId trg)]
                               (when (overlaying? src %)
                                 (yes {:src src :part src-part :entity src-ent} {:src trg :part trg-part :entity trg-ent})))))))))

(defn intersects? [part_ yes no]
  (fn [e]
    (let [src    (:src e)
          entity (:entity e)
          part   (:part e)
          canvas (:canvas e)]
      (when (contains? #{"end" "start"} part)
        (.forEachObject canvas
                        #(when (and (not (== % src)) (= part_ (.-refPartId %)))
                           (let [trg %
                                 src-ent (e/entity-from-src src)
                                 trg-ent (e/entity-from-src trg)
                                 src-part (.-refPartId src)
                                 trg-part (.-refPartId trg)]
                              (if (overlaying? src %)
                                (yes {:src src :part src-part :entity src-ent} {:src trg :part trg-part :entity trg-ent})
                                (no  {:src src :part src-part :entity src-ent} {:src trg :part trg-part :entity trg-ent})))))))))

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
                                        :top  (+ (.-top (:src part)) movementY)}))
            (.setCoords (:src part))))
        (doseq [relation (:relationships entity)]
            (let [end (:end relation)
                  related-entity (e/entity-by-id (:entity-id relation))
                  part (e/entity-part related-entity end)]
                (position-endpoint related-entity end (-> part :src (.-left) (+ movementX))
                                                      (-> part :src (.-top)  (+ movementY)))))))))

(defn all [ & handlers]
  (fn [e]
    (doseq [handler handlers]
      (handler e))))

(defn for-entity [f]
  (fn [e]
    (let [entity (:entity e)]
      (f entity))))

(defn relations-validate [entity]
  (doseq [relation (:relationships entity)]
    (let [end (:end relation)
          end-part   (e/entity-part entity end)
          end-src    (:src end-part)
          related-entity (e/entity-by-id (:entity-id relation))
          cnt (count (:parts related-entity))
          i (atom 0)]
        (doseq [part (:parts related-entity)]
          (let [related-part-src (:src part)]
            (if (not (overlaying? end-src related-part-src))
              (swap! i inc))))
        (when (= @i cnt)
          (e/disconnect-entities entity related-entity)))))


(defn align-center [src trg]
  (let [srcCx   (+ (.-left src) (/ (.-width src) 2))
        srcCy   (+ (.-top src) (/ (.-width src) 2))
        trgLeft (- srcCx (/ (.-width trg) 2))
        trgTop  (- srcCy (/ (.-height trg) 2))]
      (.set trg (clj->js {:left trgLeft :top trgTop}))
      (.setCoords trg)))

(defn position-endpoint
  ([entity terminator-end left top]
   (let [terminator-part   (e/entity-part entity terminator-end)
         relation-part     (e/entity-part entity "connector")
         arrow             (e/entity-part entity "arrow")]
    (.set (:src terminator-part) (clj->js {:left left
                                           :top  top}))
    (.setCoords (:src terminator-part))
    (.set (:src relation-part)
      (if (= "start" terminator-end)
        (clj->js {:x1 (+ (.-left (:src terminator-part)) (/ (.-width  (:src terminator-part)) 2))
                  :y1 (+ (.-top (:src terminator-part)) (/ (.-height (:src terminator-part)) 2))})
        (clj->js {:x2 (+ (.-left (:src terminator-part)) (/ (.-width  (:src terminator-part)) 2))
                  :y2 (+ (.-top (:src terminator-part)) (/ (.-height (:src terminator-part)) 2))})))
    (.setCoords (:src relation-part))

    (if (= "end" terminator-end)
      (.set (:src arrow) (clj->js {:left (.-x2 (:src relation-part))
                                   :top  (.-y2 (:src relation-part))}))
      (.setCoords (:src arrow)))
    (let [x1 (-> relation-part :src (.-x1))
          y1 (-> relation-part :src (.-y1))
          x2 (-> relation-part :src (.-x2))
          y2 (-> relation-part :src (.-y2))]
       (.set (:src arrow) (clj->js {:angle (calculate-angle x1 y1 x2 y2)}))))))


(defn toggle-connectors [entity toggle]
  (doseq [part (:parts entity)]
    (when (contains? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (:name part))
      (let [src (:src part)]
         (.set src (clj->js {:visible toggle :borderColor "#ff0000"}))))))

(defn moving-endpoint []
   (fn [e]
      (let [src (:src e)
            endpoint (:part e)
            entity (:entity e)]
         (position-endpoint entity endpoint (.-left src) (.-top  src)))))

(defmulti connector (fn [point & {:keys [moveable display visible]}] display))

(defmethod connector "circle" [point & {:keys [moveable display visible opacity]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))
                        :visible visible
                        :opacity opacity}
                       HANDLER_SMALL
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Circle. (clj->js options))))

(defmethod connector "rect" [point & {:keys [moveable display visible]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))
                        :width (* 2 (:radius HANDLER_SMALL))
                        :height (* 2 (:radius HANDLER_SMALL))
                        :visible visible}
                       (dissoc HANDLER_SMALL :radius)
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Rect. (clj->js options))))

(defn arrow [data options]
  (let [x1 (+ (:left options))
        y1 (+ (:top options))
        x2 (+ (:left options) (first (last (partition 2 data))))
        y2 (+ (:top options)  (last  (last (partition 2 data))))
        cX (/ (+ x1 x2) 2)
        cY (/ (+ y1 y2) 2)
        deltaX (- x1 cX)
        deltaY (- y1 cY)]
      (js/fabric.Triangle. (clj->js (merge {:left (+ x2 deltaX)
                                            :top (+ y1 deltaY)
                                            :originX "center"
                                            :originY "center"
                                            :angle 90
                                            :width 20
                                            :height 20}
                                           RESTRICTED_BEHAVIOUR
                                           NO_DEFAULT_CONTROLS
                                           DEFAULT_STROKE
                                           DEFAULT_FILL)))))

(defn calculate-angle [x1 y1 x2 y2]
   (let [PI 3.14
         x (- x2 x1)
         y (- y2 y1)
         angle (if (= x 0)
                   (if (= y 0)
                     0
                     (if (> y 0)
                       (/ PI 2)
                       (/ (* PI 3) 2)))
                   (if (= y 0)
                      (if (> x 0)
                          0
                          PI)
                      (if (< x 0)
                        (+ (js/Math.atan (/ y x)) PI)
                        (if (< y 0)
                          (+ (js/Math.atan (/ y x)) (* 2 PI))
                          (js/Math.atan (/ y x))))))]
      (+ (/ (* angle 180) PI) 90)))

(defentity rectangle-node data options
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
      ["connector-left"   (connector conL :moveable false :display "rect" :visibile false)
       "connector-right"  (connector conR :moveable false :display "rect" :visibile false)
       "connector-top"    (connector conT :moveable false :display "rect" :visibile false)
       "connector-bottom" (connector conB :moveable false :display "rect" :visibile false)
       "body"             (js/fabric.Rect. (clj->js enriched-opts))]))
  (with-behaviours
    ["body" "object:moving" (moving-entity "body")
     "body" "mouse:over" (highlight true DEFAULT_OPTIONS)
     "body" "mouse:out"  (highlight false DEFAULT_OPTIONS)]))


(defentity relation data options
  (with-drawables
    (let [enriched-opts (merge options DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS)
          offset-x (:left options)
          offset-y (:top options)
          points-pairs (partition 2 data)
          points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
          conS (first points-pairs-offset)
          conE (last points-pairs-offset)]
        ["connector" (js/fabric.Line. (clj->js (flatten points-pairs-offset)) (clj->js enriched-opts))
         "start"     (connector conS :moveable true :display "circle" :visible true :opacity 1)
         "arrow"     (arrow data options)
         "end"       (connector conE :moveable true :display "circle" :visible true :opacity 0)]))
  (with-behaviours
    ["start" "object:moving"  (all (moving-endpoint)
                                   (intersects? "body" (fn [src trg] (toggle-connectors (:entity trg) true))
                                                       (fn [src trg] (toggle-connectors (:entity trg) false))))
     "start" "mouse:up"       (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) (:part src))
                                                                                                                                           (toggle-connectors (:entity trg) false)
                                                                                                                                           (position-endpoint (:entity src) "start" (.-left (:src trg)) (.-top (:src trg)))))
                                   (for-entity relations-validate))
     "start" "mouse:over"     (highlight true DEFAULT_OPTIONS)
     "start" "mouse:out"      (highlight false DEFAULT_OPTIONS)

     "end"   "object:moving"  (all (moving-endpoint)
                                   (intersects? "body" (fn [src trg] (toggle-connectors (:entity trg) true))
                                                       (fn [src trg] (toggle-connectors (:entity trg) false))))
     "end"   "mouse:up"       (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) (:part src))
                                                                                                                                           (toggle-connectors (:entity trg) false)
                                                                                                                                           (position-endpoint (:entity src) "end" (.-left (:src trg)) (.-top (:src trg)))))
                                   (for-entity relations-validate))
     "end" "mouse:over"       (highlight true DEFAULT_OPTIONS)
     "end" "mouse:out"        (highlight false DEFAULT_OPTIONS)]))

(defn create
  ([entity data]
   (fn [context]
     (entity data context)))
  ([entity]
   (fn [context]
     (entity nil context))))
