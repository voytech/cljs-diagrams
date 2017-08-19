(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.general :as utils :refer [make-js-property]]))

(declare get-entity-component)
(declare get-attribute-value)

(defonce drawables (atom {}))

(defonce entities (atom {}))

(defonce lookups (atom {}))

(defonce attributes (atom {}))

(defonce entity-events (atom {}))

(defonce attribute-events (atom {}))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defrecord AttributeDomain [value
                            factory])
(defrecord Attribute [name
                      cardinality
                      index
                      domain
                      bbox
                      sync
                      factory])

(defrecord AttributeValue [id attribute value components])

(defrecord Component [name type drawable props])

(defrecord Entity [uid
                   type
                   components
                   attributes
                   relationships
                   content-bbox])

(defn entity-by-id [id]
 (get @entities id))

(defn- define-lookup [drawable-id parent]
  (let [entity-id (:entity parent)
        component-name (:component parent)
        attribute-name (:attribute parent)]
    (let [lookup (merge (or (get @lookups drawable-id) {}) parent)]
      (assoc lookups drawable-id lookup))))

(defn- define-lookups [entity]
  (doseq [component (:components entity)]
    (let [uid (:drawable component)]
      (define-lookup uid {:entity (:uid entity)
                          :component (:name component)})))
  (doseq [attribute (:attributes entity)]
    (doseq [component (:components attribute)]
      (let [aid (:id attribute)
            cid (:name component)
            did (:drawable component)]
        (define-lookup did {:entity (:uid entity)
                            :component cid
                            :attribute aid})))))

(defmulti do-lookup (fn [lookup-for entity id] lookup-for))

(defmethod do-lookup :entity [lookup-for entity id]
  entity)

(defmethod do-lookup :component [lookup-for entity id]
  (get-entity-component entity id))

(defmethod do-lookup :attribute [lookup-for entity id]
  (get-attribute-value entity id))

(defn lookup [drawable lookup-for]
  (let [uid (-> drawable :uid)
        entity-id (:entity @lookups)
        entity    (entity-by-id entity-id)
        lookup (get @lookups uid)]
    (when-let [id (lookup-for lookup)]
       (do-lookup lookup-for id))))

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type components content-bbox]
   (let [uid (str (random-uuid))
         entity (Entity. uid type components [] [] content-bbox)]
     (define-lookups entity)
     (swap! entities assoc uid entity)
     (get @entities uid)))
  ([type components]
   (create-entity type components nil)))

(defn components [holder]
  (vals (:drawables holder)))

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

(defn get-entity-content-bbox [entity]
    (:content-bbox entity))

(defn update-component-prop [entity name prop value]
  (swap! entities assoc-in [(:uid entity) :components name :props prop] value))

(defn remove-component-prop [entity name prop]
  (swap! entities update-in [(:uid entity) :components name :props ] dissoc prop))

(defn get-entity-component [entity name]
  (get-in @entities [(:uid entity) :components name]))

(defmulti register-event-handler (fn [class type component event handler] class))

(defmethod register-event-handler :entity [class type component event handler]
  (when (nil? (get-in @entity-events [type component event]))
    (swap! entity-events assoc-in [type component event] handler)))

(defmethod register-event-handler :attribute [class type component event handler]
  (when (nil? (get-in @attribute-events [type component event]))
    (swap! attribute-events assoc-in [type component event] handler)))

(defn add-entity-component [entity & components]
  (doseq [component (flatten components)]
    (swap! drawables assoc (-> component :drawable :uid) (:drawable component))
    (swap! entities assoc-in [(:uid entity) :components (:name component)] component))
  (define-lookups (entity-by-id (:uid entity))))

(defn remove-entity-component [entity component-name]
  (swap! entities update-in [(:uid entity) :components ] dissoc component-name))
  ;(eventbus/fire ""))

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
    (doseq [component components]
      (swap! drawables assoc (-> component :drawable :uid) (:drawable component)))
    (AttributeValue. (str (random-uuid)) attribute data components-map)))

(defn add-entity-attribute-value [entity & attributes]
  (doseq [attribute-value (vec attributes)]
    (let [entity-fetch (entity-by-id (:uid entity))
          existing-cardinality (count (filter #(= (-> % :attribute :name) (-> attribute-value :attribute :name)) (:attributes entity-fetch)))
          cardinality (:cardinality (:attribute attribute-value))]
      (if (> cardinality existing-cardinality)
        (do
          (let [attributes (conj (:attributes entity-fetch) attribute-value)
                sorted (sort-by #(:index (:attribute %)) attributes)]
             (swap! entities assoc-in [(:uid entity) :attributes] sorted)
             (define-lookups (entity-by-id (:uid entity)))))
        (throw (js/Error. "Trying to add more attribute values than specified attribute definition cardinality!"))))))

(defn get-attribute-value [entity id]
  (first (filter #(= (:id %) id) (:attributes entity))))

(defn get-attribute-value-component
  ([attribute-value component-name]
   (get (:components attribute-value) component-name))
  ([entity attr-id component-name]
   (get-attribute-value-component (get-attribute-value entity attr-id) component-name)))

(defn get-attribute-value-drawable [attribute-value component-name]
  (:drawable (get-attribute-value-component attribute-value component-name)))
