(ns cljs-diagrams.core.entities
  (:require [cljs-diagrams.core.eventbus :as bus]
            [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.state :as state]
            [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.utils.general :as utils :refer [make-js-property]]))

(spec/def ::entity (spec/keys :req-un [::uid
                                       ::bbox
                                       ::type
                                       ::tags
                                       ::components
                                       ::relationships
                                       ::layouts
                                       ::components-properties]))

(spec/def ::create-entity (spec/keys :req-un [::bbox
                                              ::type
                                              ::tags
                                              ::components-properties]))

(declare get-entity-component)

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defn components-of [holder]
 (vals (:components holder)))

(defn entity-by-id [app-state id]
 (state/get-in-diagram-state app-state [:entities id]))

(defn entity-by-type [type])

(defn is-entity [target]
  (spec/valid? ::entity target))

;;Utility functions for getting expected data on type non-deterministic argument
(defn- id [input fetch]
  (cond
    (is-entity input) (fetch input)
    (string? input) input))

(defn- record [input fetch]
  (cond
    (is-entity input) input
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

(defn lookup [app-state component]
  (let [uid (:parent-ref component)]
    (entity-by-id app-state uid)))

(defn create-entity
  "Creates editable entity. Entity is a first class functional element used within relational-designer.
   Entity consists of components which are building blocks for entities. Components defines drawable elements which can interact with
   each other within entity and across other entities. Component adds properties (or hints) wich holds state and allow to implement different behaviours.
   Those properties models functions of specific component."
  ([app-state type tags bbox component-properties]
   (let [uid (str (random-uuid))
         entity {:uid uid
                 :bbox bbox
                 :type type
                 :tags tags
                 :components {}
                 :relationships []
                 :layouts {}
                 :components-properties component-properties}]
     (state/assoc-diagram-state app-state [:entities uid] entity)
     (bus/fire app-state "entity.added" {:entity entity})
     entity))
  ([app-state type bbox]
   (create-entity app-state type [] bbox {})))

(defn import-entity [app-state entity-data]
  {:pre [spec/valid? ::entity entity-data]}
  (state/assoc-diagram-state app-state [:entities (:uid entity-data)] entity-data)
  (bus/fire app-state "entity.imported" {:entity entity}))


(defn remove-entity [app-state entity]
  (let [entity (entity-by-id app-state (:uid entity))]
    ;remove-relations
    (remove-entity-components app-state entity some?)
    (state/dissoc-diagram-state app-state [:entities (:uid entity)])))

(defn set-bbox [app-state entity bbox]
  (state/assoc-diagram-state app-state [:entities (:uid entity) :bbox] bbox)
  (let [updated (entity-by-id app-state (:uid entity))]
    (bus/fire app-state "entity.bbox.set" {:entity updated})
    updated))

(defn add-entity-component [app-state entity args-map]
   (let [entity (d/new-component app-state entity args-map)]
     (state/assoc-diagram-state app-state [:entities (:uid entity)] entity)
     (let [updated (entity-by-id app-state (:uid entity))]
       (bus/fire app-state "entity.component.added" {:entity updated})
       updated)))

(defn remove-entity-component [app-state entity component-name]
  (let [component (get-entity-component entity component-name)]
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

(defn preset-component-properties [entity name]
  (get-in entity [:components-properties name]))

(defn get-entity-component
 ([entity name-or-type]
  (if (keyword? name-or-type)
   (filter #(= name-or-type (:type %)) (components-of entity))
   (get-in entity [:components name-or-type])))
 ([app-state entity name-or-type]
  (get-entity-component (entity-by-id app-state (:uid entity)) name-or-type)))

(defn assert-component
 ([func app-state entity name data]
  (let [entity (entity-by-id app-state (:uid entity))
        component (get-entity-component entity name)]
    (if (nil? component)
      (func app-state entity {:name name :model data})
      (d/set-data component data))
    (get-entity-component app-state entity name)))
 ([func app-state entity args-map]
  (let [entity (entity-by-id app-state (:uid entity))
        component (get-entity-component entity (:name args-map))]
    (if (nil? component)
      (func app-state entity args-map)
      (d/set-data component (:model args-map)))
    (get-entity-component app-state entity (:name args-map)))))

(defn add-layout
  ([app-state entity layout]
   (state/assoc-diagram-state app-state [:entities (:uid entity) :layouts (:name layout)] layout)
   (let [updated (entity-by-id app-state (:uid entity))]
     (console.log (clj->js updated))
     (bus/fire app-state "entity.layout.added" {:entity updated})
     updated))
  ([app-state entity name layout-func position size margins]
   (let [layout (l/layout name layout-func position size margins)]
     (state/assoc-diagram-state app-state [:entities (:uid entity) :layouts (:name layout)] layout)
     (let [updated (entity-by-id app-state (:uid entity))]
       (bus/fire app-state "entity.layout.added" {:entity updated})
       updated)))
  ([app-state entity name layout-func position margins]
   (add-layout app-state entity name layout-func position (l/match-parent-size) margins))
  ([app-state entity name layout-func position]
   (add-layout app-state entity name layout-func position (l/match-parent-size) nil))
  ([app-state entity name layout-func]
   (add-layout app-state entity name layout-func (l/match-parent-position) (l/match-parent-size) margins)))

(defn remove-layout [app-state entity layout-name]
  (state/dissoc-diagram-state app-state [:entities (:uid entity) :layouts layout-name]))

(defn get-layout [app-state entity layout-name]
  (state/get-in-diagram-state app-state [:entities (:uid entity) :layouts layout-name]))

(defn assert-layout [app-state entity name layout-func position size margins]
  (let [layout (get-layout app-state entity name)]
    (if (nil? layout)
      (add-layout app-state entity name layout-func position size margins)
      (let [modified (-> layout
                         (assoc :position position)
                         (assoc :size size)
                         (assoc :margins margins)
                         (assoc :layout-func layout-func))]
        (state/assoc-diagram-state app-state [:entities (:uid entity) :layouts name] modified)))))

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
