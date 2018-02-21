(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.eventbus :as bus]
            [core.components :as d]
            [core.utils.general :as utils :refer [make-js-property]]))

(declare get-entity-component)
(declare get-attribute-value)
(declare get-attribute-value-component)

(defonce entities (atom {}))

(defonce lookups (volatile! {}))

(defonce attributes (atom {}))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defrecord AttributeDomain [value
                            factory])
(defrecord Attribute [name
                      cardinality
                      index
                      domain
                      sync
                      bbox
                      factory])

(defrecord AttributeValue [id attribute value components])

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

(defn- attribute-id [input]
  (id input :name))

(defn- attribute-record [input]
  (record input #(get @attributes %)))

(defn- attribute-value-id [input]
  (id input :id))

(defn- attribute-value-record [input entity]
  (record input #(get-in @entities [(entity-id entity) :attributes %])))

(defn- define-lookup [drawable-id parent]
  (let [lookup (merge (or (get @lookups drawable-id) {}) parent)]
    (vswap! lookups assoc drawable-id lookup)))

(defn- define-lookups-on-entities [entity]
  (doseq [component (vals (:components entity))]
    (let [uid (:uid component)]
      (define-lookup uid {:entity (:uid entity)}))))

(defn- define-lookups-on-attributes [entity]
  (doseq [attribute (vals (:attributes entity))]
    (doseq [component (vals (:components attribute))]
      (let [cid (:uid  component)]
        (define-lookup cid {:entity (:uid entity)
                            :attribute (:id attribute)})))))

(defn- define-lookups [entity]
  (define-lookups-on-entities entity)
  (define-lookups-on-attributes entity))

(defmulti do-lookup (fn [lookup-for entity lookup] lookup-for))

(defmethod do-lookup :entity [lookup-for entity lookup]
  entity)

(defmethod do-lookup :attribute [lookup-for entity lookup]
  (get-attribute-value entity (:attribute lookup)))

(defn lookup [component lookup-for]
  (let [uid (if (record? component) (:uid component) component)
        lookup (get @lookups uid)
        entity (entity-by-id (:entity lookup))]
    (do-lookup lookup-for entity lookup)))

(defn create-entity
  "Creates editable entity. Entity is a first class functional element used within relational-designer.
   Entity consists of components which are building blocks for entities. Components defines drawable elements which can interact with
   each other within entity and across other entities. Component adds properties (or hints) wich holds state and allow to implement different behaviours.
   Those properties models functions of specific component. Under Component we have only one Drawable wich holds properties for renderer."
  ([type components layouts]
   (let [uid (str (random-uuid))
         _components (apply merge (mapv (fn [e] {(:name e) (d/Component. (:name e) (:type e) (:drawable e) (:props e))}) components))
         entity (Entity. uid type _components {} [] layouts)]
     (define-lookups-on-entities entity)
     (swap! entities assoc uid entity)
     (bus/fire "entity.added" {:entity entity})
     (get @entities uid)))
  ([type components]
   (create-entity type components nil))
  ([type]
   (create-entity type [] nil)))

(defn add-entity-component [entity & components]
 (doseq [component (flatten components)]
   (swap! entities assoc-in [(:uid entity) :components (:name component)] component))
 (let [entity-reloaded (entity-by-id (:uid entity))]
   (define-lookups-on-entities entity-reloaded)
   (bus/fire "entity.component.added" {:entity entity-reloaded})))

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
      (add-entity-component entity (d/new-component type name data))
      (d/set-data component data))
    (get-entity-component entity name)))
 ([entity name type]
  (assert-component entity name type {})))

(defn connect-entities [src trg association-type arg1 arg2]
  (let [src-rel (conj (:relationships src) {:relation-type association-type :association-data arg1 :entity-id (:uid trg)})
        trg-rel (conj (:relationships trg) {:relation-type association-type :association-data arg2 :entity-id (:uid src)})]
    (swap! entities assoc-in [(:uid src) :relationships] src-rel)
    (swap! entities assoc-in [(:uid trg) :relationships] trg-rel)))

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

(defn get-attribute [name]
  (get @attributes name))

(defn is-attribute [name]
  (not (nil? (get-attribute name))))

(defn add-attribute [attribute]
  (when-not (is-attribute (:name attribute))
    (swap! attributes assoc-in [(:name attribute)] attribute)))

(defn create-attribute-value [attribute_ data options]
  (let [attribute (get-attribute (:name attribute_))
        domain (:domain attribute)
        domain-value (when (not (nil? domain)) (first (filter #(= data (:value %)) domain)))
        component-factory (or (:factory domain-value) (:factory attribute))
        components (component-factory data options)
        components-map (let [temp-map (into {} (map (fn [d] {(:name d) d}) components))]
                         (into (sorted-map-by (d/z-index-compare temp-map)) temp-map))
        result (AttributeValue. (str (random-uuid)) attribute data components-map)]
    (bus/fire "attribute-value.created" {:attribute-value result})
    result))

;TODO Should rahter internally invoke create-attribute-value which should be not public instead of taking attribute-value as an argument.
(defn add-entity-attribute-value [entity & attributes]
  (doseq [attribute-value (vec attributes)]
    (let [entity-fetch (entity-by-id (:uid entity))
          existing-cardinality (count (filter #(= (-> % :attribute :name) (-> attribute-value :attribute :name)) (vals (:attributes entity-fetch))))
          cardinality (:cardinality (:attribute attribute-value))]
      (if (> cardinality existing-cardinality)
        (let [attributes (:attributes entity-fetch)
              sorted  (assoc attributes (:id attribute-value) attribute-value)] ;(sorted-attributes-map (assoc attributes (:id attribute-value) attribute-value))]
           (swap! entities assoc-in [(:uid entity) :attributes] sorted)
           (define-lookups-on-attributes (entity-by-id (:uid entity))))
        (throw (js/Error. "Trying to add more attribute values than specified attribute definition cardinality!"))))))

(defn remove-attribute-value [entity-or-id attribute-value-or-id]
  (let [eid (entity-id entity-or-id)
        avid (attribute-value-id attribute-value-or-id)
        attribute-value (get-in @entities [eid :attributes avid])]
    (doseq [component (components-of attribute-value)]
      (d/remove-component component))
    (swap! entities update-in [eid :attributes] dissoc avid)
    (entity-by-id eid)))

(defn get-attribute-value [entity id]
  (get-in @entities [(:uid entity) :attributes id]))

(defn get-attributes-values [entity]
  (vals (:attributes (entity-by-id (:uid entity)))))

(defn get-attribute-value-component
  ([attribute-value component-name]
   (get (:components attribute-value) component-name))
  ([entity attr-id component-name]
   (get-attribute-value-component (get-attribute-value entity attr-id) component-name)))

(defn get-attribute-value-property [attribute-value component-name property]
  (let [drawable (get-attribute-value-component attribute-value component-name)]
    (d/getp drawable property)))

(defn get-attribute-value-data [attribute-value]
  (:value attribute-value))

;(defn- cached-create-attribute-value []
;  (memoize create-attribute-value))

(defn- replace-attribute-value [entity attribute-value new-value]
  (-> entity
      (remove-attribute-value attribute-value)
      (add-entity-attribute-value (create-attribute-value (:attribute attribute-value) new-value))))

(defn- update-attribute-value-value [entity attribute-value new-value]
  (let [eid  (entity-id entity)
        avid (attribute-value-id attribute-value)
        attribute (:attribute (attribute-value-record attribute-value entity))]
    (swap! entities assoc-in [eid :attributes avid :value] new-value)
    (when-let [sync (:sync attribute)]
      (sync (get-attribute-value entity avid)))))

(defn update-attribute-value [entity-or-id attribute-value-or-id value]
  (let [entity (entity-record entity-or-id)
        attribute-value (attribute-value-record attribute-value-or-id entity)
        attribute (:attribute attribute-value)]
    (if (not (nil? (:domain attribute)))
      (replace-attribute-value entity attribute-value value)
      (update-attribute-value-value entity attribute-value value))
    (bus/fire "layout.do" {:container (volatile-entity entity) :type :attributes})))
