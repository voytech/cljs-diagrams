(ns core.behaviour-api
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.components :as d]
            [core.eventbus :as b]
            [core.events :as ev]
            [core.behaviours :as bhv]
            [impl.components :as c]))

(defn- center-point [cmp]
  (let [mx (+ (d/get-left cmp) (/ (d/get-width cmp) 2))
        my (+ (d/get-top cmp) (/ (d/get-height cmp) 2))]
    {:x mx :y my}))

(defn- distance [p1 p2]
  (js/Math.sqrt (+ (js/Math.pow (- (:x p2) (:x p1)) 2) (js/Math.pow (- (:y p2) (:y p1)) 2))))

(defn to-the-center-of [component x y target]
  (when (not (nil? component))
    (d/set-data component {x (+ (d/get-left target) (/ (d/get-width target) 2))
                           y (+ (d/get-top target) (/ (d/get-height target) 2))})))

(defn component-hover [component bln options]
  (d/set-data component {:border-color (if bln (:hover-color options)
                                               (:normal-color options))
                         :border-width (if bln (:hover-width options)
                                               (:normal-width options))}))

(defn show [app-state entity component-name show]
 (let [component (e/get-entity-component app-state entity component-name)]
   (d/setp component :visible show)))

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

(defn move-related-entity [app-state entity related-entity relation left top]
  (let [event-data {:entity related-entity
                    :relation relation
                    :app-state app-state
                    :movement-x left
                    :movement-y top}]
     (bhv/trigger-behaviour app-state related-entity nil nil "moveby" event-data)))

(defn default-position-related-entity [app-state entity related-entity relation left top]
  (move-related-entity app-state entity related-entity relation left top))

(defn is-relation-owner [entity relation]
  (= (:owner relation) (:uid entity)))

(defn owned-relationships [entity]
  (filterv #(is-relation-owner entity %) (:relationships entity)))

(defn move-entity [app-state component movement-x movement-y]
  (let [entity (e/lookup app-state component)
        event (:event e)]
    (default-position-entity app-state
                             entity
                             (:name component)
                             movement-x
                             movement-y
                             :offset)
    (doseq [relation (owned-relationships entity)]
      (let [related-entity (e/entity-by-id app-state (:entity-id relation))]
         (default-position-related-entity app-state
                                          entity
                                          related-entity
                                          relation
                                          movement-x
                                          movement-y)))))

(defn collides?
  ([app-state component feature hit-callback miss-callback]
    (let [entity (e/lookup app-state component)
          collisions (filterv (fn [trg-comp]
                                (and (not= trg-comp component)
                                     (not= (:parentRef trg-comp) (:parentRef component))
                                     (d/intersects? component trg-comp)
                                     (feature (e/lookup app-state trg-comp))))
                              (d/ordered-components app-state))]
          (if-let [collider (first collisions)]
            (hit-callback {:component component :entity entity}
                          {:component collider :entity (e/lookup app-state collider)})
            (miss-callback {:component component :entity entity}))))
  ([app-state component hit-callback miss-callback]
    (collides? app-state component #(true) hit-callback miss-callback)))

(defn includes?
  ([app-state component feature hit-callback miss-callback])
  ([app-state component hit-callback miss-callback]))

(defn collision-based-relations-validate
  ([app-state entity]
    (let [source-entity (e/entity-by-id app-state (:uid entity))
          source-components (e/components-of source-entity)]
      (doseq [relation (:relationships source-entity)]
        (let [related-entity (e/entity-by-id app-state (:entity-id relation))
              related-components (e/components-of related-entity)
              result (->> (for [component source-components
                                related related-components]
                            (d/intersects? related component))
                          (reduce #(or %1 %2) false))]
          (when (false? result)
            (e/disconnect-entities app-state entity related-entity)            
            ))))))

(defn inclusion-based-relations-validate [app-state entity]
  )
