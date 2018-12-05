(ns impl.behaviours.standard-api
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.components :as d]
            [core.eventbus :as b]
            [core.events :as ev]
            [core.behaviours :as bhv]
            [impl.components :as c]))

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
   (d/set-data (:component e) {:border-color (if bln (:highlight-color options)
                                                     (:normal-color options))
                               :border-width (if bln (:highlight-width options)
                                                     (:normal-width options))})))

(defn show [app-state entity component-name show]
 (let [component (e/get-entity-component app-state entity component-name)]
   (d/setp component :visible show)))

(defn intersects-controls? [yes]
 (fn [e]
   (let [entity           (:entity e)
         app-state         (-> e :app-state)
         component        (:component e)]
     (when (contains? #{"end" "start"} (:name component))
       (doseq [trg-comp (vals (get-in @app-state [:diagram :components]))]
          (when (and (not= trg-comp component) (= ::c/control (:type trg-comp)))
            (let [trg-ent (e/lookup app-state trg-comp)]
              (when (d/intersects? component trg-comp)
                (yes {:component component :entity entity} {:component trg-comp :entity trg-ent})))))))))

(defn intersects? [target-name yes no]
 (fn [e]
   (let [entity           (:entity e)
         app-state         (-> e :app-state)
         component        (:component e)]
     (when (contains? #{"end" "start"} (:name component))
       (doseq [trg-comp (vals (get-in @app-state [:diagram :components]))]
          (when (= target-name (:name trg-comp))
            (let [trg-ent  (e/lookup app-state trg-comp :entity)]
               (if (d/intersects? component trg-comp)
                 (yes {:component component :entity entity} {:component trg-comp :entity trg-ent})
                 (no  {:component component :entity entity} {:component trg-comp :entity trg-ent})))))))))

(defn calculate-offset [component left top]
 {:left (- left (d/getp component :left))
  :top  (- top  (d/getp component :top))})

(defn calculate-effective-offset [app-state entity component-name left top coord-mode]
 (if (= :offset coord-mode)
   {:left left :top top}
   (let [component (e/get-entity-component app-state entity component-name)]
     (calculate-offset component left top))))

(defn effective-position
 ([component get-x get-y x y coord-mode]
  (let [effective-x (if (= :offset coord-mode) (+ (get-x component) x) x)
        effective-y  (if (= :offset coord-mode) (+ (get-y component) y) y)]
    {:x effective-x :y effective-y}))
 ([component x y coord-mode]
  (effective-position component #(d/getp % :left) #(d/getp % :top) x y coord-mode)))

(defn apply-effective-position
 ([component set-x get-x set-y get-y x y coord-mode]
  (let [epos (effective-position component get-x get-y x y coord-mode)]
    (set-x component (:x epos))
    (set-y component (:y epos))))
 ([comopnent x y coord-mode]
  (apply-effective-position comopnent
                            #(d/setp %1 :left %2)
                            #(d/getp % :left)
                            #(d/setp %1 :top %2)
                            #(d/getp % :top)
                            x
                            y
                            coord-mode)))

(defn default-position-entity-component [app-state entity component-name left top coord-mode]
  (let [component (e/get-entity-component app-state entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defn default-position-entity [app-state entity ref-component-name left top coord-mode]
 (let [effective-offset (calculate-effective-offset app-state entity ref-component-name left top coord-mode)]
   (doseq [component (e/components-of entity)]
     (let [effective-left  (+ (d/getp component :left) (:left effective-offset))
           effective-top   (+ (d/getp component :top) (:top effective-offset))]
       (if (= ref-component-name (:name component))
         (default-position-entity-component app-state entity (:name component) left top :offset)
         (default-position-entity-component app-state entity (:name component) effective-left effective-top :absolute))))))

(defn move-related-entity [app-state entity related-entity relation left top coord-mode]
  (let [event-data {:entity related-entity
                    :relation relation
                    :app-state app-state
                    :movement-x left
                    :movement-y top}]
     (bhv/trigger-behaviour related-entity nil nil "moveby" event-data)))

(defn default-position-related-entity [app-state entity related-entity relation left top coord-mode]
  (move-related-entity app-state entity related-entity relation left top coord-mode))

(defn moving-entity []
 (fn [e]
   (let [entity (:entity e)
         event (:event e)
         app-state (:app-state e)
         component (:component e)
         movementX (:movement-x e)
         movementY (:movement-y e)]
     (default-position-entity app-state
                              entity
                              (:name component)
                              movementX
                              movementY
                              :offset)
     (doseq [relation (:relationships entity)]
       (let [related-entity (e/entity-by-id app-state (:entity-id relation))]
          (default-position-related-entity   app-state
                                             entity
                                             related-entity
                                             relation
                                             movementX
                                             movementY
                                             :offset))))))

(defn relations-validate [app-state entity]
 (doseq [relation (:relationships entity)]
   (let [related-entity (e/entity-by-id app-state (:entity-id relation))
         relation-type (:relation-type relation)
         component-type (cond
                          (= :start relation-type) ::c/startpoint
                          (= :end relation-type) ::c/endpoint)
         endpoint (first (e/get-entity-component app-state entity component-type))
         controls (e/get-entity-component app-state related-entity ::c/control)
         component-intersections (filter #(d/intersects? % endpoint) controls)]
     (when (= 0 (count component-intersections))
       (e/disconnect-entities app-state entity related-entity)))))

(defn set-relation-movement-hook [sent-type rent-type hook]
  (swap! hooks assoc-in [sent-type rent-type] hook))

(defn refresh-arrow-angle [relation-component arrow-component]
  (when (not (nil? relation-component))
    (let [x1 (-> relation-component  (d/getp :x1))
          y1 (-> relation-component  (d/getp :y1))
          x2 (-> relation-component  (d/getp :x2))
          y2 (-> relation-component  (d/getp :y2))]
       (d/setp arrow-component :angle (calculate-angle x1 y1 x2 y2)))))

(defn- to-the-center-of [line x y shape]
  (when (not (nil? line))
    (d/set-data line {x (+ (d/get-left shape) (/ (d/get-width shape) 2))
                      y (+ (d/get-top shape) (/ (d/get-height shape) 2))})))

(defn insert-breakpoint []
  (fn [e]
      (let [entity (:entity e)
            line (:component e)
            app-state (-> e :app-state)
            line-start-breakpoint (e/get-entity-component app-state entity (:start (:props line)))
            line-end-breakpoint   (e/get-entity-component app-state entity (:end (:props line)))
            oeX  (d/getp line :x2)
            oeY  (d/getp line :y2)
            eX   (:left e)
            eY   (:top e)]
        (when (= ::c/relation (:type line))
          (d/set-data line {:x2 eX :y2 eY})
          (let [relation-id   (str (random-uuid))
                breakpoint-id (str (random-uuid))
                is-penultimate (= true (:penultimate (:props line-start-breakpoint)))]
            (-> entity
              (e/add-entity-component app-state ::c/relation relation-id  {:x1 eX :y1 eY :x2 oeX :y2 oeY} {:start breakpoint-id :end (:name line-end-breakpoint)})
              (e/add-entity-component app-state ::c/breakpoint breakpoint-id {:point [eX eY]} {:end (:name line) :start relation-id :penultimate is-penultimate}))
            (e/update-component-prop app-state entity (:name line) :end breakpoint-id)
            (e/update-component-prop app-state entity (:name line-end-breakpoint) :end relation-id)
            (when (= true is-penultimate)
              (e/update-component-prop app-state entity (:name line-start-breakpoint) :penultimate false)))))))

(defn dissoc-breakpoint []
  (fn [e]
    (let [entity     (:entity e)
          breakpoint (:component e)
          app-state (-> e :app-state)
          line-end   (e/get-entity-component app-state entity (:start  (:props breakpoint)))
          line-endpoint (e/get-entity-component app-state entity (:end (:props line-end)))
          line-start (e/get-entity-component app-state entity (:end   (:props breakpoint)))
          line-startpoint (e/get-entity-component app-state entity (:start (:props line-start)))
          is-penultimate? (:penultimate (:props breakpoint))]
       (e/remove-entity-component app-state entity (:name breakpoint))
       (e/remove-entity-component app-state entity (:name line-end))
       (e/update-component-prop app-state entity (:name line-start) :end (:name line-endpoint))
       (e/update-component-prop app-state entity (:name line-endpoint) :end (:name line-start))
       (e/update-component-prop app-state entity (:name line-startpoint) :penultimate is-penultimate?)
       (d/set-data drawable {:x2 (+ (d/getp line-endpoint :left) (/ (d/getp line-endpoint :width) 2))
                             :y2 (+ (d/getp line-endpoint :top) (/ (d/getp line-endpoint :height) 2))}))))

(defn position-breakpoint
  ([app-state entity name left top coord-mode]
   (let [breakpoint-component (e/get-entity-component app-state entity name)
         position (effective-position app-state breakpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component app-state entity (:start (:props breakpoint-component)))
         ends-relation-component (e/get-entity-component app-state entity (:end (:props breakpoint-component)))
         arrow-component (e/get-entity-component app-state entity "arrow")]
     (d/set-data  breakpoint-component {:left effective-left :top  effective-top})
     (to-the-center-of starts-relation-component  :x1 :y1 breakpoint-component)
     (to-the-center-of ends-relation-component :x2 :y2 breakpoint-component)
     (when (= true (:penultimate (:props breakpoint-component)))
       (refresh-arrow-angle starts-relation-component arrow-component))))
  ([app-state entity name left top]
   (position-breakpoint app-state entity name left top :absolute)))

(defn position-startpoint
  ([app-state entity left top coord-mode skip?]
   (let [startpoint-component (e/get-entity-component app-state entity "start")
         position (effective-position startpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component app-state entity (:start (:props startpoint-component)))
         arrow-component (e/get-entity-component app-state entity "arrow")]
     (d/set-data startpoint-component {:left effective-left :top  effective-top})
     (when (= false skip?)
      (to-the-center-of starts-relation-component :x1 :y1 startpoint-component)
      (when (= true (:penultimate (:props startpoint-component)))
        (refresh-arrow-angle starts-relation-component arrow-component)))))
  ([app-state entity left top]
   (position-startpoint app-state entity left top :absolute false)))

(defn position-endpoint
  ([app-state entity left top coord-mode skip?]
   (let [endpoint-component  (e/get-entity-component app-state entity "end")
         position (effective-position endpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         ends-relation-component  (e/get-entity-component app-state entity (:end (:props endpoint-component)))

         arrow-component      (e/get-entity-component app-state entity "arrow")]
    (d/set-data endpoint-component {:left effective-left  :top  effective-top})
    (to-the-center-of arrow-component :left :top endpoint-component)
    (when (= false skip?)
     (to-the-center-of ends-relation-component :x2 :y2 endpoint-component)
     (refresh-arrow-angle ends-relation-component arrow-component))))
 ([app-state entity left top]
  (position-endpoint app-state entity left top :absolute false)))

(defn toggle-controls [entity toggle]
  (doseq [component (e/components-of entity)]
    (when (= ::c/control (:type component))
       (d/set-data component {:visible toggle :border-color "#ff0000"}))))

(defn toggle-control [control toggle]
  (when (= ::c/control (:type component))
      (d/set-data control {:visible toggle :border-color "#ff0000"})))

(defn moving-endpoint []
   (fn [e]
      (let [endpoint (:component e)
            entity   (:entity e)
            app-state (-> e :app-state)]
         (cond
           (= ::c/breakpoint (:type endpoint)) (position-breakpoint app-state entity (:name endpoint) (:movement-x e) (:movement-y e) :offset)
           (= ::c/startpoint (:type endpoint)) (position-startpoint app-state entity (:movement-x e) (:movement-y e) :offset false)
           (= ::c/endpoint   (:type endpoint)) (position-endpoint   app-state entity (:movement-x e) (:movement-y e) :offset false)))))

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
