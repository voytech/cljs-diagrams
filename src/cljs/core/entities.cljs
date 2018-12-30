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
                   bbox
                   type
                   tags
                   components
                   relationships
                   layout-definition
                   groups
                   components-properties
                   shape-ref])

(defn components-of [holder]
 (vals (:components holder)))

(defn entity-by-id [app-state id]
 (state/get-in-diagram-state app-state [:entities id]))

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
  (let [entities (state/get-in-diagram-state app-state [:entities])]
    (record input #(get-in entities [(entity-id entity) :components %]))))

(defn is-entity [target]
  (instance? Entity target))

(defn lookup [app-state component]
  (let [uid (:parent-ref component)]
    (entity-by-id app-state uid)))

(defn create-entity
  "Creates editable entity. Entity is a first class functional element used within relational-designer.
   Entity consists of components which are building blocks for entities. Components defines drawable elements which can interact with
   each other within entity and across other entities. Component adds properties (or hints) wich holds state and allow to implement different behaviours.
   Those properties models functions of specific component."
  ([app-state type tags layout-definition bbox component-properties shape-ref]
     (let [uid (str (random-uuid))
           entity (Entity. uid
                           bbox
                           type
                           tags
                           {} []
                           layout-definition
                           {} ;groups
                           component-properties
                           shape-ref)]
       (state/assoc-diagram-state app-state [:entities uid] entity)
       (bus/fire app-state "entity.added" {:entity entity})
       entity))
  ([app-state type layout-definition size]
   (create-entity app-state type [] layout-definition size {} nil))
  ([app-state type layout-definition]
   (create-entity app-state type layout-definition {:width 180 :height 150})))

(defn add-entity-component
  ([app-state entity type name data props method]
    (add-entity-component app-state entity type name data props method nil))
  ([app-state entity type name data props method initializer]
    (add-entity-component app-state entity type name data props method initializer nil))
  ([app-state entity type name data props method initializer layout-hints]
    (let [entity (d/new-component app-state entity layout-hints type name data props method initializer)]
      (state/assoc-diagram-state app-state [:entities (:uid entity)] entity)
      (let [updated (entity-by-id app-state (:uid entity))]
        (bus/fire app-state "entity.component.added" {:entity updated})
        updated))))

(defn remove-entity-component [app-state entity component-name]
  (let [component (get-entity-component app-state entity component-name)]
    (state/dissoc-diagram-state app-state [:entities (:uid entity) :components component-name])
    (d/remove-component app-state component)))

(defn remove-entity-components [app-state entity pred]
  (let [all (components-of (entity-by-id app-state (:uid entity)))
        filtered-out (filterv pred all)]
    (doseq [rem filtered-out]
      (remove-entity-component app-state entity (:name rem)))))

(defn update-component-attribute [app-state entity name attribute value]
 (state/assoc-diagram-state app-state [:entities (:uid entity) :components name :attributes attribute] value))

(defn remove-component-attribute [app-state entity name attribute]
 (state/dissoc-diagram-state app-state [:entities (:uid entity) :components name :attributes attribute]))

(defn component-attribute [app-state entity name attribute]
  (state/get-in-diagram-state app-state [:entities (:uid entity) :components name :attributes attribute]))

(defn get-entity-component [app-state entity name-or-type]
  (if (keyword? name-or-type)
   (filter #(= name-or-type (:type %)) (components-of (entity-by-id app-state (:uid entity))))
   (state/get-in-diagram-state app-state [:entities (:uid entity) :components name-or-type])))

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

(defn add-group
  ([app-state entity name layout-func relative-bbox margins]
    (let [group (l/ComponentGroup.
                   (str (random-uuid))
                   (l/LayoutDefinition.
                     name layout-func relative-bbox margins)
                   []
                   (:uid entity))]
      (state/assoc-diagram-state app-state [:entities (:uid entity) :groups (:name layout-definition)] group)
      (let [updated (entity-by-id app-state (:uid entity))]
        (bus/fire app-state "entity.group.added" {:entity updated})
        updated)))
  ([app-state entity name layout-func relative-bbox]
    (add-group app-state entity name layout-func relative-bbox nil)))

(defn remove-group [app-state entity group-name]
  (state/dissoc-diagram-state app-state [:entities (:uid entity) :groups group-name]))

(defn get-group [app-state entity group-name]
  (state/get-in-diagram-state app-state [:entities (:uid entity) :groups group-name]))

(defn assert-group [app-state entity name layout-func relative-bbox margins]
  (let [group (get-group app-state entity name)]
    (if (nil? group)
      (add-group app-state entity name layout-func relative-bbox margins)
      (let [modified (-> group
                         (assoc :relative-bbox relative-bbox)
                         (assoc :margins margins)
                         (assoc :layout-func layout-func))]
        (state/assoc-diagram-state app-state [:entities (:uid entity) :groups name] modified)))))

(defn- is-relation-present [app-state entity related-id assoc-type]
  (->> (entity-by-id app-state (:uid entity))
       :relationships
       (filterv (fn [rel] (and (= related-id (:entity-id rel)) (= assoc-type (:relation-type rel)))))
       (count)
       (< 0)))

(defn connect-entities [app-state src trg association-type]
  (when (not (is-relation-present app-state src (:uid trg) association-type))
    (let [src-rel (conj (:relationships src) {:relation-type association-type :entity-id (:uid trg) :owner (:uid src)})
          trg-rel (conj (:relationships trg) {:relation-type association-type :entity-id (:uid src) :owner (:uid src)})]
      (state/assoc-diagram-state app-state [:entities (:uid src) :relationships] src-rel)
      (state/assoc-diagram-state app-state [:entities (:uid trg) :relationships] trg-rel))))

(defn get-related-entities [app-state entity association-type]
  (let [_entity (volatile-entity app-state entity)]
    (->> (:relationships _entity)
         (filter  #(= (:relation-type %) association-type))
         (mapv #(entity-by-id app-state (:entity-id %))))))

(defn disconnect-entities
  ([app-state src trg]
   (let [src-rel (filter #(not= (:uid trg) (:entity-id %)) (:relationships src))
         trg-rel (filter #(not= (:uid src) (:entity-id %)) (:relationships trg))]
     (state/assoc-diagram-state app-state [:entities (:uid src) :relationships] src-rel)
     (state/assoc-diagram-state app-state [:entities (:uid trg) :relationships] trg-rel)))
  ([app-state src trg association-type]
   (let [src-rel (filter #(and (not= (:relation-type %) association-type)
                               (not= (:uid trg) (:entity-id %))) (:relationships src))
         trg-rel (filter #(and (not= (:relation-type %) association-type)
                               (not= (:uid src) (:entity-id %))) (:relationships trg))]
     (state/assoc-diagram-state app-state [:entities (:uid src) :relationships] src-rel)
     (state/assoc-diagram-state app-state [:entities (:uid trg) :relationships] trg-rel))))
