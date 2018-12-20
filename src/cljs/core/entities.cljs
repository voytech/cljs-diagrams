(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.eventbus :as bus]
            [core.components :as d]
            [core.state :as state]
            [core.utils.general :as utils :refer [make-js-property]]))

(declare get-entity-component)

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defrecord Entity [uid
                   size
                   type
                   tags
                   components
                   relationships
                   layouts
                   components-properties
                   shape-ref])

(defn components-of [holder]
 (vals (:components holder)))

(defn entity-by-id [app-state id]
 (get (:entities (state/diagram-state app-state)) id))

(defn entity-by-type [type])

;;Utility functions for getting expected data on type non-deterministic argument
(defn- id [input fetch]
  (cond
    (record? input) (fetch input)
    (string? input) input))

(defn- record [input fetch]
  (cond
    (record? input) input
    (string? input) (fetch input)))

(defn- entity-id [input]
  (id input :uid))

(defn- entity-record [app-state input]
  (record input #(entity-by-id app-state %)))

(defn volatile-entity [app-state entity]
  (->> entity
       entity-id
       (entity-record app-state)))

(defn- component-id [input]
  (id input :name))

(defn- component-record [app-state input entity]
  (let [entities (:entities (state/diagram-state app-state))]
    (record input #(get-in entities [(entity-id entity) :components %]))))

(defn is-entity [target]
  (instance? Entity target))

(defn lookup [app-state component]
  (let [uid (:parentRef component)]
    (entity-by-id app-state uid)))

(defn create-entity
  "Creates editable entity. Entity is a first class functional element used within relational-designer.
   Entity consists of components which are building blocks for entities. Components defines drawable elements which can interact with
   each other within entity and across other entities. Component adds properties (or hints) wich holds state and allow to implement different behaviours.
   Those properties models functions of specific component."
  ([app-state type tags layouts size component-properties shape-ref]
     (let [uid (str (random-uuid))
           entity (Entity. uid
                           size
                           type
                           tags
                           {} []
                           layouts
                           component-properties
                           shape-ref)]
       (swap! app-state assoc-in [:diagram :entities uid] entity)
       (bus/fire app-state "entity.added" {:entity entity})
       entity))
  ([app-state type layouts size]
   (create-entity app-state type [] layouts size {} nil))
  ([app-state type layouts]
   (create-entity app-state type layouts {:width 180 :height 150}))
  ([app-state type]
   (create-entity app-state type [] {:width 180 :height 150})))

(defn add-entity-component
  ([app-state entity type name data props method]
    (add-entity-component app-state entity type name data props method nil))
  ([app-state entity type name data props method initializer]
    (let [entity (d/new-component app-state entity type name data props method initializer)]
      (swap! app-state assoc-in [:diagram :entities (:uid entity)] entity)
      (let [updated (entity-by-id app-state (:uid entity))]
        (bus/fire app-state "entity.component.added" {:entity updated})
        updated))))

(defn remove-entity-component [app-state entity component-name]
  (let [component (get-entity-component app-state entity component-name)]
    (swap! app-state update-in [:diagram :entities (:uid entity) :components] dissoc component-name)
    (d/remove-component app-state component)))

(defn remove-entity-components [app-state entity pred]
  (let [all (components-of (entity-by-id app-state (:uid entity)))
        filtered-out (filterv pred all)]
    (doseq [rem filtered-out]
      (remove-entity-component app-state entity (:name rem)))))

(defn update-component-prop [app-state entity name prop value]
 (swap! app-state assoc-in [:diagram :entities (:uid entity) :components name :props prop] value))

(defn remove-component-prop [app-state entity name prop]
 (swap! app-state assoc-in [:diagram :entities (:uid entity) :components name :props ] dissoc prop))

(defn component-property [app-state entity name prop]
  (get-in (state/diagram-state app-state) [:entities (:uid entity) :components name :props prop]))

(defn get-entity-component [app-state entity name-or-type]
  (if (keyword? name-or-type)
   (filter #(= name-or-type (:type %)) (components-of (entity-by-id app-state (:uid entity))))
   (get-in @app-state [:diagram :entities (:uid entity) :components name-or-type])))

(defn get-shape-component [app-state entity]
  (when-let [shape-name (:shape-ref entity)]
    (get-entity-component app-state entity shape-name)))

(defn assert-component
 ([func app-state entity name data]
  (let [component (get-entity-component app-state entity name)]
    (if (nil? component)
      (func app-state entity name data {})
      (d/set-data component data))
    (get-entity-component app-state entity name)))
 ([func app-state entity name]
  (assert-component func app-state entity name {})))

(defn- is-relation-present [app-state entity related-id assoc-type]
  (->> (entity-by-id app-state (:uid entity))
       :relationships
       (filterv (fn [rel] (and (= related-id (:entity-id rel)) (= assoc-type (:relation-type rel)))))
       (count)
       (< 0)))

(defn connect-entities [app-state src trg association-type]
  (when (not (is-relation-present app-state src (:uid trg) association-type))
    (let [src-rel (conj (:relationships src) {:relation-type association-type :entity-id (:uid trg)})
          trg-rel (conj (:relationships trg) {:relation-type association-type :entity-id (:uid src)})]
      (swap! app-state assoc-in [:diagram :entities (:uid src) :relationships] src-rel)
      (swap! app-state assoc-in [:diagram :entities (:uid trg) :relationships] trg-rel))))

(defn get-related-entities [app-state entity association-type]
  (let [_entity (volatile-entity app-state entity)]
    (->> (:relationships _entity)
         (filter  #(= (:relation-type %) association-type))
         (mapv #(entity-by-id app-state (:entity-id %))))))

(defn disconnect-entities
  ([app-state src trg]
   (let [src-rel (filter #(not= (:uid trg) (:entity-id %)) (:relationships src))
         trg-rel (filter #(not= (:uid src) (:entity-id %)) (:relationships trg))]
     (swap! app-state assoc-in [:diagram :entities (:uid src) :relationships] src-rel)
     (swap! app-state assoc-in [:diagram :entities (:uid trg) :relationships] trg-rel)))
  ([app-state src trg association-type]
   (let [src-rel (filter #(and (not= (:relation-type %) association-type)
                               (not= (:uid trg) (:entity-id %))) (:relationships src))
         trg-rel (filter #(and (not= (:relation-type %) association-type)
                               (not= (:uid src) (:entity-id %))) (:relationships trg))]
     (swap! app-state assoc-in [:diagram :entities (:uid src) :relationships] src-rel)
     (swap! app-state assoc-in [:diagram :entities (:uid trg) :relationships] trg-rel))))
