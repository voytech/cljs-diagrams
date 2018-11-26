(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.eventbus :as bus]
            [core.components :as d]
            [core.utils.general :as utils :refer [make-js-property]]))

(declare get-entity-component)

(defonce entities (atom {}))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defrecord Entity [uid
                   type
                   components
                   attributes
                   relationships
                   layouts])

(defn components-of [holder]
 (vals (:components holder)))

(defn entity-by-id [id]
 (get @entities id))

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

(defn- entity-record [input]
  (record input entity-by-id))

(defn volatile-entity [entity]
  (-> entity
      entity-id
      entity-record))

(defn- component-id [input]
  (id input :name))

(defn- component-record [input entity]
  (record input #(get-in @entities [(entity-id entity) :components %])))

(defn is-entity [target]
  (instance? Entity target))

(defn lookup [component]
  (let [uid (:parentRef component)]
    (entity-by-id uid)))

(defn create-entity
  "Creates editable entity. Entity is a first class functional element used within relational-designer.
   Entity consists of components which are building blocks for entities. Components defines drawable elements which can interact with
   each other within entity and across other entities. Component adds properties (or hints) wich holds state and allow to implement different behaviours.
   Those properties models functions of specific component."
  ([type layouts]
   (let [uid (str (random-uuid))
         entity (Entity. uid type {} {} [] layouts)]
     (swap! entities assoc uid entity)
     (bus/fire "entity.added" {:entity entity})
     (get @entities uid)))
  ([type]
   (create-entity type nil)))

(defn add-entity-component
  ([entity type name data props method]
    (->> (d/new-component entity type name data props method)
         (swap! entities assoc (:uid entity)))
    (let [entity-reloaded (entity-by-id (:uid entity))]
      (bus/fire "entity.component.added" {:entity entity-reloaded})
      entity-reloaded))
  ([entity type name data props]
   (add-entity-component entity type name data props nil))
  ([entity type name data]
   (add-entity-component entity type name data {} nil))
  ([entity type name]
   (add-entity-component entity type name {} {} nil)))

(defn remove-entity-component [entity component-name]
  (let [component (get-in @entities [(:uid entity) :components component-name])]
    (d/remove-component component)
    (swap! entities update-in [(:uid entity) :components ] dissoc component-name)))

(defn remove-entity-components [entity pred]
  (let [all (components-of (entity-by-id (:uid entity)))
        filtered-out (filterv pred all)]
    (doseq [rem filtered-out] (d/remove-component rem))
    (when-not (empty? filtered-out)
      (swap! entities assoc-in [(:uid entity) :components] (apply dissoc (:components (entity-by-id (:uid entity))) (mapv :name filtered-out))))))

(defn update-component-prop [entity name prop value]
 (swap! entities assoc-in [(:uid entity) :components name :props prop] value))

(defn remove-component-prop [entity name prop]
 (swap! entities update-in [(:uid entity) :components name :props ] dissoc prop))

(defn component-property [entity name prop]
  (get-in @entities [(:uid entity) :components name :props prop]))

(defn get-entity-component [entity name-or-type]
  (if (keyword? name-or-type)
   (filter #(= name-or-type (:type %)) (components-of (entity-by-id (:uid entity))))
   (get-in @entities [(:uid entity) :components name-or-type])))

(defn assert-component
 ([entity name type data]
  (let [component (get-entity-component entity name)]
    (if (or (nil? component) (not= type (:type component)))
      (add-entity-component entity type name data)
      (d/set-data component data))
    (get-entity-component entity name)))
 ([entity name type]
  (assert-component entity name type {})))

(defn connect-entities [src trg association-type]
  (let [src-rel (conj (:relationships src) {:relation-type association-type :entity-id (:uid trg)})
        trg-rel (conj (:relationships trg) {:relation-type association-type :entity-id (:uid src)})]
    (swap! entities assoc-in [(:uid src) :relationships] src-rel)
    (swap! entities assoc-in [(:uid trg) :relationships] trg-rel)))

(defn get-related-entities [entity association-type]
  (let [_entity (volatile-entity entity)]
    (->> (:relationships _entity)
         (filter  #(= (:relation-type %) association-type))
         (mapv #(entity-by-id (:entity-id %))))))

(defn disconnect-entities
  ([src trg]
   (let [src-rel (filter #(not= (:uid trg) (:entity-id %)) (:relationships src))
         trg-rel (filter #(not= (:uid src) (:entity-id %)) (:relationships trg))]
     (swap! entities assoc-in [(:uid src) :relationships] src-rel)
     (swap! entities assoc-in [(:uid trg) :relationships] trg-rel)))
  ([src trg association-type]
   (let [src-rel (filter #(and (not= (:relation-type %) association-type)
                               (not= (:uid trg) (:entity-id %))) (:relationships src))
         trg-rel (filter #(and (not= (:relation-type %) association-type)
                               (not= (:uid src) (:entity-id %))) (:relationships trg))]
     (swap! entities assoc-in [(:uid src) :relationships] src-rel)
     (swap! entities assoc-in [(:uid trg) :relationships] trg-rel))))

(defn index-of [coll v]
  (let [i (count (take-while #(not= v %) coll))]
    (when (or (< i (count coll))
            (= v (last coll)))
      i)))
