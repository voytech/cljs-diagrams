(ns core.entities-behaviours
 (:require [core.entities :as e]
           [core.project :as p]
           [core.rendering :as r]
           [core.layouts :as layouts]
           [core.drawables :as d]
           [core.eventbus :as b]
           [clojure.string :as str]))


(declare position-endpoint)
(declare position-startpoint)
(declare position-breakpoint)
(declare moving-endpoint)
(declare relation-line)
(declare endpoint)
(declare calculate-angle)
(declare dissoc-breakpoint)

(defn intersects-any? [names yes]
  (fn [e]
    (let [entity           (:entity e)
          component-name   (:component e)
          component        (e/get-entity-component entity component-name)
          drawable         (:drawable component)]
      (when (contains? #{"end" "start"} component-name)
        (doseq [drwlb @r/drawables]
          #(when (and (not (== drwlb drawable)) (= :endpoint (:type (e/get-drawable-parent-component drwlb))))
             (let [trg-ent  (e/get-drawable-parent-entity drwlb)
                   trg-comp (e/get-drawable-parent-component drwlb)]
               (when (intersects? drawable drwlb)
                 (yes {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})))))))))

(defn intersects? [target-name yes no]
  (fn [e]
    (let [entity           (:entity e)
          component-name   (:component e)
          component        (e/get-entity-component entity component-name)
          drawable         (:drawable component)]
      (when (contains? #{"end" "start"} component-nam)
        (doseq [drwlb @r/drawables]
          #(when (and (not (== drwlb drawable)) (= :endpoint (:type (e/get-drawable-parent-component drwlb))))
             (let [trg-ent  (e/get-drawable-parent-entity drwlb)
                   trg-comp (e/get-drawable-parent-component drwlb)]
                (if (intersects? drawable drwlb)
                  (yes {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})
                  (no  {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})))))))))

(defn- assert-position-context [context]
  (when-not (or (= context :entity-scope)
                (= context :relation-scope)
                (= context :any-scope))
     (Error. "context must be entity-relation or component-relation")))

(defn- calculate-offset [component left top]
  {:left (- left (d/getp (:drawable component) :left))
   :top  (- top  (d/getp (:drawable component) :top))})

(defn- calculate-effective-offset [entity component-name left top coord-mode]
  (if (= :offset coord-mode)
    {:left left :top top}
    (let [component (e/get-entity-component entity component-name)]
      (calculate-offset component left top))))

(defn- effective-position
 ([component get-x get-y x y coord-mode]
  (let [effective-x (if (= :offset coord-mode) (+ (get-x (:drawable component)) x) x)
        effective-y  (if (= :offset coord-mode) (+ (get-y (:drawable component)) y) y)]
    {:x effective-x :y effective-y}))
 ([component x y coord-mode]
  (effective-position component #(d/getp % :left) #(d/getp % :top) x y coord-mode)))

(defn- apply-effective-position
 ([component set-x get-x set-y get-y x y coord-mode]
  (let [epos (effective-position component get-x get-y x y coord-mode)]
    (set-x (:drawable component) (:x epos))
    (set-y (:drawable component) (:y epos))))
 ([comopnent x y coord-mode]
  (apply-effective-position comopnent
                            #(d/setp %1 :left %2)
                            #(d/getp % :left)
                            #(d/setp %1 :top %2)
                            #(d/getp % :top)
                            x
                            y
                            coord-mode)))

(defn- position-attributes-components [attributes offset-left offset-top]
   (doseq [src (flatten (mapv #(e/components %) attributes))]
     (d/set-data (:drawable src) {:left (+ (d/getp (:drawable src) :left) offset-left)
                                  :top  (+ (d/getp (:drawable src) :top) offset-top)})))

(defmulti get-refered-component-name (fn [relation] (:type (e/entity-by-id (:entity-id relation)))))

(defmulti position-entity-component (fn [entity component-name context left top coord-mode]
                                     (assert-position-context context)
                                     [(:type entity) (:type (e/get-entity-component entity component-name)) context]))

(defmulti position-entity (fn [entity ref-component-name context left top coord-mode]
                            (assert-position-context context)
                            [(:type entity) context]))

;; ==============================================================================================
;; Multimethods for RECTANGLE-NODE entity positioning
;; ==============================================================================================

(defmethod get-refered-component-name "rectangle-node" [relation]
   "body")

(defmethod position-entity-component [ "rectangle-node" :main :entity-scope] [entity component-name context left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defmethod position-entity-component [ "rectangle-node" :endpoint :entity-scope] [entity component-name context left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defmethod position-entity ["rectangle-node" :entity-scope] [entity ref-component-name context left top coord-mode]
  (let [effective-offset (calculate-effective-offset entity ref-component-name left top coord-mode)]
    (doseq [component (e/components entity)]
      (let [effective-left  (+ (d/getp (:drawable component) :left) (:left effective-offset))
            effective-top   (+ (d/getp (:drawable component) :top) (:top effective-offset))]
        (when-not (= (:name component) ref-component-name)
          (position-entity-component entity (:name component) context effective-left effective-top :absolute))))
    (position-attributes-components (:attributes entity) (:left effective-offset) (:top effective-offset))))

(defmethod position-entity-component [ "rectangle-node" :main :relation-scope] [entity component-name context left top coord-mode])

(defmethod position-entity-component [ "rectangle-node" :endpoint :relation-scope] [entity component-name context left top coord-mode])

(defmethod position-entity ["rectangle-node" :relation-scope] [entity ref-component-name context left top coord-mode])

;; ==============================================================================================
;; Multimethods for RELATION entity positioning
;; ==============================================================================================

(defmethod get-refered-component-name "relation" [relation]
   (:association-data relation))

(defmethod position-entity-component [ "relation" :endpoint   :relation-scope] [entity component-name context left top coord-mode]
  (position-endpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :startpoint :relation-scope] [entity component-name context left top coord-mode]
  (position-startpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :breakpoint :relation-scope] [entity component-name context left top coord-mode]
  (position-breakpoint entity component-name left top coord-mode))

(defmethod position-entity [ "relation" :relation-scope] [entity ref-component-name context left top coord-mode]
  (position-entity-component entity ref-component-name context left top coord-mode))

(defmethod position-entity-component [ "relation" :endpoint   :entity-scope] [entity component-name context left top coord-mode]
  (position-endpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :startpoint :entity-scope] [entity component-name context left top coord-mode]
  (position-startpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :breakpoint :entity-scope] [entity component-name context left top coord-mode]
  (position-breakpoint entity component-name left top coord-mode))

(defmethod position-entity-component [ "relation" :decorator  :entity-scope] [entity component-name context left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defmethod position-entity-component [ "relation" :relation  :entity-scope] [entity component-name context left top coord-mode])

(defmethod position-entity ["relation" :entity-scope] [entity ref-component-name context left top coord-mode]
  (let [effective-offset (calculate-effective-offset entity ref-component-name left top coord-mode)]
    (doseq [component (e/components entity)]
      (when-not (= (:name component) ref-component-name)
        (let [effective-left  (+ (d/get-left (:drawable component)) (:left effective-offset))
              effective-top   (+ (d/get-top (:drawable component)) (:top effective-offset))]
           (position-entity-component entity (:name component) context effective-left effective-top :absolute))))
    (position-attributes-components (:attributes entity) (:left effective-offset) (:top effective-offset))))

(defn moving-entity []
  (fn [e]
    (let [entity (:entity e)
          event (:event e)
          component-name (:component e)
          movementX (:movement-x e)
          movementY (:movement-y e)]
      (position-entity entity
                       component-name
                       :entity-scope
                       movementX
                       movementY
                       :offset)
      (doseq [relation (:relationships entity)]
          (let [related-entity (e/entity-by-id (:entity-id relation))
                ref-component-name (get-refered-component-name relation)]
             (position-entity-component related-entity
                                        ref-component-name
                                        :relation-scope
                                        movementX
                                        movementY
                                        :offset))))))

(defn assert-drawable [event name]
  (= (name (:component event))))

(defn insert-breakpoint []
  (fn [e]
    (when-not (and  (= (:type (p/prev-event e)) "object:moving")
                    (= (:component (p/prev-event e)) (:component e)))
      (let [entity (:entity e)
            line (e/get-entity-component entity (:component e))
            line-start-breakpoint (e/get-entity-component entity (:start (:props line)))
            line-end-breakpoint   (e/get-entity-component entity (:end (:props line)))
            drawable  (:drawable line)
            oeX  (d/getp drawable :x2)
            oeY  (d/getp drawable :y2)
            eX   (:x e)
            eY   (:y e)]
        (when (= :relation (:type line))
          (d/set-data drawable {:x2 eX :y2 eY})
          ;
          (let [relation-id   (str (random-uuid))
                breakpoint-id (str (random-uuid))
                is-penultimate (= true (:penultimate (:props line-start-breakpoint)))]
            (e/add-entity-component entity
              {:name     relation-id
               :type     :relation
               :drawable (relation-line eX eY oeX oeY)
               :props    {:start breakpoint-id :end (:name line-end-breakpoint)}}
              {:name     breakpoint-id
               :type     :breakpoint
               :drawable (endpoint [eX eY] :moveable true :display "circle" :visible true :opacity 1)
               :props    {:end (:name line) :start relation-id :penultimate is-penultimate}})
            (b/fire "rendering.execute" {:entity entity})
            (e/update-component-prop entity (:name line) :end breakpoint-id)
            (e/update-component-prop entity (:name line-end-breakpoint) :end relation-id)
            (when (= true is-penultimate)
              (e/update-component-prop entity (:name line-start-breakpoint) :penultimate false))))))))

(defn dissoc-breakpoint []
  (fn [e]
   (let [prev (p/prev-event)]
     (when (and  (not= (:type prev) "object:moving"))
      (let [entity     (:entity e)
            breakpoint (e/get-entity-component entity (:component e))
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
         (position-entity-component entity (:name line-endpoint) :entity-scope (d/get-left (:drawable line-endpoint))
                                                                               (d/get-top  (:drawable line-endpoint)))
         (b/fire "rendering.execute" {:entity entity}))))))

; by default validate relations depending on entity bounding box intersection rule.
(defn relations-validate [entity]
  (doseq [relation (:relationships entity)]
    (let [related-entity (e/entity-by-id (:entity-id relation))
          target-bbox (layouts/get-bbox related-entity)
          source-bbox (layouts/get-bbox entity)]
      (when (not (layouts/intersects? source-bbox target-bbox))
        (e/disconnect-entities entity related-entity)))))

(defn- refresh-arrow-angle [relation-component arrow-component]
  (let [x1 (-> relation-component :drawable (d/getp :x1))
        y1 (-> relation-component :drawable (d/getp :y1))
        x2 (-> relation-component :drawable (d/getp :x2))
        y2 (-> relation-component :drawable (d/getp :y2))]
     (d/setp (:drawable arrow-drawable) :angle (calculate-angle x1 y1 x2 y2))))

(defn- to-the-center-of [line x y shape]
  (d/set-data line {x (+ (d/get-left shape) (/ (d/get-width shape) 2))
                    y (+ (d/get-top shape) (/ (d/get-height shape) 2))}))

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
    (when (contains? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (:name component))
      (let [drawable (:drawable component)]
         (d/set-data drawable {:visible toggle :border-color "#ff0000"})))))

(defn moving-endpoint []
   (fn [e]
      (let [endpoint-name (:component e)
            entity   (:entity e)
            endpoint (e/get-entity-component entity endpoint-name)
            drawable (:drawable endpoint)]
         (cond
           (= :breakpoint (:type endpoint)) (position-breakpoint entity endpoint-name (d/get-left drawable) (d/get-top drawable))
           (= :startpoint (:type endpoint)) (position-startpoint entity (d/get-left drawable) (d/get-top drawable))
           (= :endpoint   (:type endpoint)) (position-endpoint   entity (d/get-left drawable) (d/get-top drawable))))))

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
