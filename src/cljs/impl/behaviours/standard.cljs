(ns impl.behaviours.standard
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [core.behaviours :refer [effective-position default-position-entity-component]]
            [impl.drawables :as dimpl]))

(declare position-endpoint)
(declare position-startpoint)
(declare position-breakpoint)
(declare calculate-angle)
(declare dissoc-breakpoint)

(defn- refresh-arrow-angle [relation-component arrow-component]
  (let [x1 (-> relation-component :drawable (d/getp :x1))
        y1 (-> relation-component :drawable (d/getp :y1))
        x2 (-> relation-component :drawable (d/getp :x2))
        y2 (-> relation-component :drawable (d/getp :y2))]
     (d/setp (:drawable arrow-component) :angle (calculate-angle x1 y1 x2 y2))))

(defn- to-the-center-of [line x y shape]
  (d/set-data line {x (+ (d/get-left shape) (/ (d/get-width shape) 2))
                    y (+ (d/get-top shape) (/ (d/get-height shape) 2))}))

(defn insert-breakpoint []
  (fn [e]
      (let [entity (:entity e)
            line (:component e)
            line-start-breakpoint (e/get-entity-component entity (:start (:props line)))
            line-end-breakpoint   (e/get-entity-component entity (:end (:props line)))
            drawable  (:drawable line)
            oeX  (d/getp drawable :x2)
            oeY  (d/getp drawable :y2)
            eX   (:left e)
            eY   (:top e)]
        (when (= :relation (:type line))
          (d/set-data drawable {:x2 eX :y2 eY})
          (let [relation-id   (str (random-uuid))
                breakpoint-id (str (random-uuid))
                is-penultimate (= true (:penultimate (:props line-start-breakpoint)))]
            (e/add-entity-component entity
              {:name     relation-id
               :type     :relation
               :drawable (dimpl/relation-line eX eY oeX oeY)
               :props    {:start breakpoint-id :end (:name line-end-breakpoint)}}
              {:name     breakpoint-id
               :type     :breakpoint
               :drawable (dimpl/endpoint [eX eY] :moveable true :display "circle" :visible true :opacity 1)
               :props    {:end (:name line) :start relation-id :penultimate is-penultimate}})
            (e/update-component-prop entity (:name line) :end breakpoint-id)
            (e/update-component-prop entity (:name line-end-breakpoint) :end relation-id)
            (when (= true is-penultimate)
              (e/update-component-prop entity (:name line-start-breakpoint) :penultimate false)))))))

(defn dissoc-breakpoint []
  (fn [e]
    (let [entity     (:entity e)
          breakpoint (:component e)
          line-end   (e/get-entity-component entity (:start  (:props breakpoint)))
          line-endpoint (e/get-entity-component entity (:end (:props line-end)))
          line-start (e/get-entity-component entity (:end   (:props breakpoint)))
          line-startpoint (e/get-entity-component entity (:start (:props line-start)))
          is-penultimate? (:penultimate (:props breakpoint))]
       (e/remove-entity-component entity (:name breakpoint))
       (e/remove-entity-component entity (:name line-end))
       (e/update-component-prop entity (:name line-start) :end (:name line-endpoint))
       (e/update-component-prop entity (:name line-endpoint) :end (:name line-start))
       (e/update-component-prop entity (:name line-startpoint) :penultimate is-penultimate?)
       (let [drawable (:drawable line-start)
             endpoint-drawable (:drawable line-endpoint)]
         (d/set-data drawable {:x2 (+ (d/getp endpoint-drawable :left) (/ (d/getp endpoint-drawable :width) 2))
                               :y2 (+ (d/getp endpoint-drawable :top) (/ (d/getp endpoint-drawable :height) 2))})))))

(defn position-breakpoint
  ([entity name left top coord-mode]
   (let [breakpoint-component (e/get-entity-component entity name)
         position (effective-position breakpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component entity (:start (:props breakpoint-component)))
         ends-relation-component (e/get-entity-component entity (:end (:props breakpoint-component)))
         arrow-component (e/get-entity-component entity "arrow")]
     (d/set-data (:drawable breakpoint-component)  {:left effective-left :top  effective-top})
     (to-the-center-of (:drawable starts-relation-component) :x1 :y1 (:drawable breakpoint-component))
     (to-the-center-of (:drawable ends-relation-component)  :x2 :y2 (:drawable breakpoint-component))
     (when (= true (:penultimate (:props breakpoint-component)))
       (refresh-arrow-angle starts-relation-component arrow-component))))
  ([entity name left top]
   (position-breakpoint entity name left top :absolute)))

(defn position-startpoint
  ([entity left top coord-mode]
   (let [startpoint-component (e/get-entity-component entity "start")
         position (effective-position startpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component entity (:start (:props startpoint-component)))
         arrow-component (e/get-entity-component entity "arrow")]
     (d/set-data (:drawable startpoint-component) {:left effective-left :top  effective-top})
     (to-the-center-of (:drawable starts-relation-component) :x1 :y1 (:drawable startpoint-component))
     (when (= true (:penultimate (:props startpoint-component)))
       (refresh-arrow-angle starts-relation-component arrow-component))))
  ([entity left top]
   (position-startpoint entity left top :absolute)))

(defn position-endpoint
  ([entity left top coord-mode]
   (let [endpoint-component   (e/get-entity-component entity "end")
         position (effective-position endpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         ends-relation-component  (e/get-entity-component entity (:end (:props endpoint-component)))

         arrow-component      (e/get-entity-component entity "arrow")]
    (d/set-data (:drawable endpoint-component) {:left effective-left  :top  effective-top})
    (to-the-center-of (:drawable ends-relation-component) :x2 :y2 (:drawable endpoint-component))
    (to-the-center-of (:drawable arrow-component) :left :top (:drawable endpoint-component))
    (refresh-arrow-angle ends-relation-component arrow-component)))
 ([entity left top]
  (position-endpoint entity left top :absolute)))

(defn toggle-endpoints [entity toggle]
  (doseq [component (e/components entity)]
    (when (= :endpoint (:type component))
      (let [drawable (:drawable component)]
         (d/set-data drawable {:visible toggle :border-color "#ff0000"})))))

(defn moving-endpoint []
   (fn [e]
      (let [endpoint (:component e)
            entity   (:entity e)
            drawable (:drawable endpoint)]
         (cond
           (= :breakpoint (:type endpoint)) (position-breakpoint entity (:name endpoint) (:movement-x e) (:movement-y e) :offset)
           (= :startpoint (:type endpoint)) (position-startpoint entity (:movement-x e) (:movement-y e) :offset)
           (= :endpoint   (:type endpoint)) (position-endpoint   entity (:movement-x e) (:movement-y e) :offset)))))

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
