(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.general :as utils :refer [make-js-property]]))


(defonce ^:private ID "refId")
(defonce ^:private PART_ID "refPartId")
(defonce ^:private ATTR_ID "refAttrId")

(defonce entities (atom {}))

(defonce paged-entities (atom {}))

(defonce attributes (atom {}))

(defonce entity-events (atom {}))

(defonce attribute-events (atom {}))

(declare js-obj-id)

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

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type components content-bbox]
   (let [uid (str (random-uuid))
         entity (Entity. uid type components [] [] content-bbox)]
     (doseq [component components]
       (make-js-property (:drawable component) ID  (:uid entity))
       (make-js-property (:drawable component) PART_ID (:name component)))
     (swap! entities assoc uid entity)
     entity))
  ([type components]
   (create-entity type components nil)))

(defn components [holder]
  (vals (:drawables holder)))

(defn bind [entity page]
  (let [euids (page @paged-entities)
        uid (:uid entity)]
    (swap! paged-entities assoc-in [page] (if (nil? euids) #{uid} (conj euids uid)))))

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

(defn entity-by-id [id]
  (get @entities id))

(defn js-obj-id [src]
  (.-refId src))

(defn entity-from-src [src]
  (-> src
      js-obj-id
      entity-by-id))

(defn entity-part-name-from-src [src]
  (.-refPartId src))

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
    (make-js-property (:src component) ID  (:uid entity))
    (make-js-property (:src component) PART_ID (:name component))
    (swap! entities assoc-in [(:uid entity) :components (:name component)] component)))

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
    (AttributeValue. (str (random-uuid)) attribute data components-map)))

(defn add-entity-attribute-value [entity & attributes]
  (doseq [attribute-value (vec attributes)]
    (let [entity-fetch (entity-by-id (:uid entity))
          existing-cardinality (count (filter #(= (-> % :attribute :name) (-> attribute-value :attribute :name)) (:attributes entity-fetch)))
          cardinality (:cardinality (:attribute attribute-value))]
      (if (> cardinality existing-cardinality)
        (do
          (doseq [component (vals (:components attribute-value))]
            (make-js-property (:drawable component) ID  (:uid entity))
            (make-js-property (:drawable component) ATTR_ID (:id attribute-value))
            (make-js-property (:drawable component) PART_ID (:name component)))
          (let [attributes (conj (:attributes entity-fetch) attribute-value)
                sorted (sort-by #(:index (:attribute %)) attributes)]
             (swap! entities assoc-in [(:uid entity) :attributes] sorted)))
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
