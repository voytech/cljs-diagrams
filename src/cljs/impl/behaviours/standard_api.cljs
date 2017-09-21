(ns impl.behaviours.standard-api
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [core.events :as ev]
            [core.behaviours :as bhv]
            [impl.drawables :as dimpl]))

(declare position-endpoint)
(declare position-startpoint)
(declare position-breakpoint)
(declare calculate-angle)
(declare dissoc-breakpoint)


(defn to-the-center-of [line x y shape]
  (d/set-data line {x (+ (d/get-left shape) (/ (d/get-width shape) 2))
                    y (+ (d/get-top shape) (/ (d/get-height shape) 2))}))

(defn highlight [bln options]
 (fn [e]
   (d/set-data (:drawable e) {:border-color (if bln (:highlight-color options)
                                                    (:normal-color options))
                              :border-width (if bln (:highlight-width options)
                                                    (:normal-width options))})))

(defn show [entity component-name show]
 (let [component (e/get-entity-component entity component-name)]
   (d/setp (:drawable component) :visible show)))

(defn intersects-controls? [yes]
 (fn [e]
   (let [entity           (:entity e)
         component        (:component e)
         drawable         (:drawable e)]
     (when (contains? #{"end" "start"} (:name component))
       (doseq [drwlb (vals @d/drawables)]
          (when (and (not= drwlb drawable) (= :control (:type (e/lookup drwlb :component))))
            (let [trg-ent  (e/lookup drwlb :entity)
                  trg-comp (e/lookup drwlb :component)]
              (when (d/intersects? drawable drwlb)
                (yes {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})))))))))

(defn intersects? [target-name yes no]
 (fn [e]
   (let [entity           (:entity e)
         component        (:component e)
         drawable         (:drawable e)]
     (when (contains? #{"end" "start"} (:name component))
       (doseq [drwlb (vals @d/drawables)]
          (when (= target-name (:name (e/lookup drwlb :component)))
            (let [trg-ent  (e/lookup drwlb :entity)
                  trg-comp (e/lookup drwlb :component)]
               (if (d/intersects? drawable drwlb)
                 (yes {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})
                 (no  {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})))))))))

(defn calculate-offset [component left top]
 {:left (- left (d/getp (:drawable component) :left))
  :top  (- top  (d/getp (:drawable component) :top))})

(defn calculate-effective-offset [entity component-name left top coord-mode]
 (if (= :offset coord-mode)
   {:left left :top top}
   (let [component (e/get-entity-component entity component-name)]
     (calculate-offset component left top))))

(defn effective-position
 ([component get-x get-y x y coord-mode]
  (let [effective-x (if (= :offset coord-mode) (+ (get-x (:drawable component)) x) x)
        effective-y  (if (= :offset coord-mode) (+ (get-y (:drawable component)) y) y)]
    {:x effective-x :y effective-y}))
 ([component x y coord-mode]
  (effective-position component #(d/getp % :left) #(d/getp % :top) x y coord-mode)))

(defn apply-effective-position
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
  (doseq [src (flatten (mapv #(e/components-of %) attributes))]
    (d/set-data (:drawable src) {:left (+ (d/getp (:drawable src) :left) offset-left)
                                 :top  (+ (d/getp (:drawable src) :top) offset-top)})))

(defn default-position-entity-component [entity component-name left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defn default-position-entity [entity ref-component-name left top coord-mode]
 (let [effective-offset (calculate-effective-offset entity ref-component-name left top coord-mode)]
   (doseq [component (e/components-of entity)]
     (let [effective-left  (+ (d/getp (:drawable component) :left) (:left effective-offset))
           effective-top   (+ (d/getp (:drawable component) :top) (:top effective-offset))]
       (if (= ref-component-name (:name component))
         (default-position-entity-component entity (:name component) left top :offset)
         (default-position-entity-component entity (:name component) effective-left effective-top :absolute))))
   (position-attributes-components (vals (:attributes entity)) (:left effective-offset) (:top effective-offset))))

(defn move-related-entity [entity related-entity relation left top coord-mode]
  (let [event-data {:entity related-entity
                    :relation relation
                    :movement-x left
                    :movement-y top}]
     (bhv/trigger-behaviour related-entity nil nil "moveby" event-data)))

(defn default-position-related-entity [entity related-entity relation left top coord-mode]
  (move-related-entity entity related-entity relation left top coord-mode))

(defn moving-entity []
 (fn [e]
   (let [entity (:entity e)
         event (:event e)
         component (:component e)
         movementX (:movement-x e)
         movementY (:movement-y e)]
     (default-position-entity entity
                              (:name component)
                              movementX
                              movementY
                              :offset)
     (doseq [relation (:relationships entity)]
       (let [related-entity (e/entity-by-id (:entity-id relation))]
          (default-position-related-entity   entity
                                             related-entity
                                             relation
                                             movementX
                                             movementY
                                             :offset))))))

(defn relations-validate [entity]
 (doseq [relation (:relationships entity)]
   (let [related-entity (e/entity-by-id (:entity-id relation))
         target-bbox (layouts/get-bbox related-entity)
         source-bbox (layouts/get-bbox entity)]
     (when (not (layouts/intersects? source-bbox target-bbox))
       (e/disconnect-entities entity related-entity)))))

(defn set-relation-movement-hook [sent-type rent-type hook]
  (swap! hooks assoc-in [sent-type rent-type] hook))

(defn refresh-arrow-angle [relation-component arrow-component]
  (when (not (nil? relation-component))
    (let [x1 (-> relation-component :drawable (d/getp :x1))
          y1 (-> relation-component :drawable (d/getp :y1))
          x2 (-> relation-component :drawable (d/getp :x2))
          y2 (-> relation-component :drawable (d/getp :y2))]
       (d/setp (:drawable arrow-component) :angle (calculate-angle x1 y1 x2 y2)))))

(defn- to-the-center-of [line x y shape]
  (when (not (nil? line))
    (d/set-data line {x (+ (d/get-left shape) (/ (d/get-width shape) 2))
                      y (+ (d/get-top shape) (/ (d/get-height shape) 2))})))

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
            (e/add-entity-component entity (e/new-component relation-id :relation {:x1 eX :y1 eY :x2 oeX :y2 oeY} {:start breakpoint-id :end (:name line-end-breakpoint)})
                                           (e/new-component breakpoint-id :breakpoint {:point [eX eY]} {:end (:name line) :start relation-id :penultimate is-penultimate}))
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
  ([entity left top coord-mode skip?]
   (let [startpoint-component (e/get-entity-component entity "start")
         position (effective-position startpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component entity (:start (:props startpoint-component)))
         arrow-component (e/get-entity-component entity "arrow")]
     (d/set-data (:drawable startpoint-component) {:left effective-left :top  effective-top})
     (when (= false skip?)
      (to-the-center-of (:drawable starts-relation-component) :x1 :y1 (:drawable startpoint-component))
      (when (= true (:penultimate (:props startpoint-component)))
        (refresh-arrow-angle starts-relation-component arrow-component)))))
  ([entity left top]
   (position-startpoint entity left top :absolute false)))

(defn position-endpoint
  ([entity left top coord-mode skip?]
   (let [endpoint-component   (e/get-entity-component entity "end")
         position (effective-position endpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         ends-relation-component  (e/get-entity-component entity (:end (:props endpoint-component)))

         arrow-component      (e/get-entity-component entity "arrow")]
    (d/set-data (:drawable endpoint-component) {:left effective-left  :top  effective-top})
    (to-the-center-of (:drawable arrow-component) :left :top (:drawable endpoint-component))
    (when (= false skip?)
     (to-the-center-of (:drawable ends-relation-component) :x2 :y2 (:drawable endpoint-component))
     (refresh-arrow-angle ends-relation-component arrow-component))))
 ([entity left top]
  (position-endpoint entity left top :absolute false)))

(defn toggle-controls [entity toggle]
  (doseq [component (e/components-of entity)]
    (when (= :control (:type component))
      (let [drawable (:drawable component)]
         (d/set-data drawable {:visible toggle :border-color "#ff0000"})))))

(defn moving-endpoint []
   (fn [e]
      (let [endpoint (:component e)
            entity   (:entity e)
            drawable (:drawable endpoint)]
         (cond
           (= :breakpoint (:type endpoint)) (position-breakpoint entity (:name endpoint) (:movement-x e) (:movement-y e) :offset)
           (= :startpoint (:type endpoint)) (position-startpoint entity (:movement-x e) (:movement-y e) :offset false)
           (= :endpoint   (:type endpoint)) (position-endpoint   entity (:movement-x e) (:movement-y e) :offset false)))))

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
