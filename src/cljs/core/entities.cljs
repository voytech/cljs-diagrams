(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.eventbus :as bus]
            [core.drawables :as d]
            [core.utils.general :as utils :refer [make-js-property]]))

(declare get-entity-component)
(declare get-attribute-value)
(declare get-attribute-value-component)

(defonce entities (atom {}))

(defonce lookups (atom {}))

(defonce attributes (atom {}))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defrecord AttributeDomain [value
                            factory])
(defrecord Attribute [name
                      cardinality
                      index
                      domain
                      bbox
                      factory])

(defrecord AttributeValue [id attribute value components])

(defrecord Component [name type drawable props])

(defrecord Entity [uid
                   type
                   components
                   attributes
                   relationships
                   content-bbox])

(defn components [holder]
 (vals (:components holder)))

(defn entity-by-id [id]
 (get @entities id))

(defn- define-lookup [drawable-id parent]
  (let [lookup (merge (or (get @lookups drawable-id) {}) parent)]
    (swap! lookups assoc drawable-id lookup)))

(defn- define-lookups-on-components [entity]
  (doseq [component (vals (:components entity))]
    (let [uid (:uid (:drawable component))]
      (define-lookup uid {:entity (:uid entity)
                          :component (:name component)}))))

(defn- define-lookups-on-attributes [entity]
  (doseq [attribute (:attributes entity)]
    (doseq [component (vals (:components attribute))]
      (let [did (:uid (:drawable component))]
        (define-lookup did {:entity (:uid entity)
                            :component (:name component)
                            :attribute (:id attribute)})))))

(defn- define-lookups [entity]
  (define-lookups-on-components entity)
  (define-lookups-on-attributes entity))

(defmulti do-lookup (fn [lookup-for entity lookup] lookup-for))

(defmethod do-lookup :entity [lookup-for entity lookup]
  entity)

(defmethod do-lookup :component [lookup-for entity lookup]
  (if (not (nil? (:attribute lookup)))
    (get-attribute-value-component entity (:attribute lookup) (:component lookup))
    (get-entity-component entity (:component lookup))))

(defmethod do-lookup :attribute [lookup-for entity lookup]
  (get-attribute-value entity (:attribute lookup)))

(defn lookup [drawable lookup-for]
  (let [uid (if (record? drawable) (:uid drawable) drawable)
        lookup (get @lookups uid)
        entity (entity-by-id (:entity lookup))]
    (do-lookup lookup-for entity lookup)))

(defn create-entity
  "Creates editable entity. Entity is a first class functional element used within relational-designer.
   Entity consists of components which are building blocks for entities. Components defines drawable elements which can interact with
   each other within entity and across other entities. Component adds properties (or hints) wich holds state and allow to implement different behaviours.
   Those properties models functions of specific component. Under Component we have only one Drawable wich holds properties for renderer."
  ([type components content-bbox]
   (let [uid (str (random-uuid))
         _components (apply merge (cons {} (mapv (fn [e] {(:name e) (Component. (:name e) (:type e) (:drawable e) (:props e))}) components)))
         entity (Entity. uid type _components [] [] content-bbox)]
     (define-lookups-on-components entity)
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
 (define-lookups-on-components (entity-by-id (:uid entity))))

(defn remove-entity-component [entity component-name]
  (let [component (get-in @entities [(:uid entity) :components component-name])
        drawable (:drawable component)]
    (d/remove-drawable drawable)
    (swap! entities update-in [(:uid entity) :components ] dissoc component-name)))

(defn update-component-prop [entity name prop value]
 (swap! entities assoc-in [(:uid entity) :components name :props prop] value))

(defn remove-component-prop [entity name prop]
 (swap! entities update-in [(:uid entity) :components name :props ] dissoc prop))

(defn get-entity-component [entity name]
 (get-in @entities [(:uid entity) :components name]))

(defn get-entity-content-bbox [entity]
   (:content-bbox entity))

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
        components-map (into {} (map (fn [d] {(:name d) d}) components))]
    (AttributeValue. (str (random-uuid)) attribute data components-map)))

(defn add-entity-attribute-value [entity & attributes]
  (doseq [attribute-value (vec attributes)]
    (let [entity-fetch (entity-by-id (:uid entity))
          existing-cardinality (count (filter #(= (-> % :attribute :name) (-> attribute-value :attribute :name)) (:attributes entity-fetch)))
          cardinality (:cardinality (:attribute attribute-value))]
      (if (> cardinality existing-cardinality)
        (let [attributes (conj (:attributes entity-fetch) attribute-value)
              sorted (sort-by #(:index (:attribute %)) attributes)]
           (swap! entities assoc-in [(:uid entity) :attributes] sorted)
           (define-lookups-on-attributes (entity-by-id (:uid entity))))
        (throw (js/Error. "Trying to add more attribute values than specified attribute definition cardinality!"))))))

(defn get-attribute-value [entity id]
  (first (filter #(= (:id %) id) (:attributes entity))))

(defn get-attributes-values [entity]
  (:attributes (entity-by-id (:uid entity))))

(defn get-attribute-value-component
  ([attribute-value component-name]
   (get (:components attribute-value) component-name))
  ([entity attr-id component-name]
   (get-attribute-value-component (get-attribute-value entity attr-id) component-name)))

(defn get-attribute-value-drawable [attribute-value component-name]
  (:drawable (get-attribute-value-component attribute-value component-name)))

(defn get-attribute-value-property [attribute-value component-name property]
  (let [drawable (:drawable (get-attribute-value-component attribute-value component-name))]
    (d/getp drawable property)))

(defn get-attribute-value-data [attribute-value]
  (:value attribute-value))
