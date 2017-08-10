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

(defrecord Attribute [name cardinality index domain bbox sync])

(defrecord AttributeValue [id attribute value img drawables])

(defrecord Drawable [name type src props])

(defprotocol IEntity
  (add-attribute [this attribute])
  (connect-to [this entity]))

(defrecord Entity [uid
                   type
                   drawables
                   attributes
                   relationships
                   content-bbox]

  IEntity
  (add-attribute [this attribute])
  (connect-to [this entity]))

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type drawables content-bbox]
   (let [uid (str (random-uuid))
         entity (Entity. uid type drawables [] [] content-bbox)]
     (doseq [drawable drawables]
       (make-js-property (:src drawable) ID  (:uid entity))
       (make-js-property (:src drawable) PART_ID (:name drawable)))
     (swap! entities assoc uid entity)
     entity))
  ([type drawables]
   (create-entity type drawables nil)))

(defn bind [entity page]
  (let [euids (page @paged-entities)
        uid (:uid entity)]
    (swap! paged-entities assoc-in [page] (if (nil? euids) #{uid} (conj euids uid)))))

(defn connect-entities [src trg end]
  (let [src-rel (conj (:relationships src) {:end end :entity-id (:uid trg)})
        trg-rel (conj (:relationships trg) {:end end :entity-id (:uid src)})]
    (swap! entities assoc-in [(:uid src) :relationships] src-rel)
    (swap! entities assoc-in [(:uid trg) :relationships] trg-rel)))

(defn disconnect-entities [src trg]
  (let [src-rel (filter #(not= (:uid trg) (:entity-id %)) (:relationships src))
        trg-rel (filter #(not= (:uid src) (:entity-id %)) (:relationships trg))]
    (swap! entities assoc-in [(:uid src) :relationships] src-rel)
    (swap! entities assoc-in [(:uid trg) :relationships] trg-rel)))

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

(defn get-entity-bbox [entity]
   (let [sources (mapv :src (:drawables entity))
         leftmost (apply min-key (cons #(.-left %) sources))
         rightmost (apply max-key (cons #(+ (.-left %) (.-width %)) sources))
         topmost (apply min-key (cons #(.-top %) sources))
         bottommost (apply max-key (cons #(+ (.-top %) (.-height %)) sources))]
     {:left (.-left leftmost)
      :top  (.-top topmost)
      :width (- (+ (.-left rightmost) (.-width rightmost)) (.-left leftmost))
      :height (- (+ (.-top bottommost) (.-height  bottommost)) (.-top topmost))}))

(defn update-drawable-prop [entity name char value]
  (let [drawable (get-entity-drawable entity name)
        i (index-of (:drawables (entity-by-id (:uid entity))) drawable)]
     (swap! entities assoc-in [(:uid entity) :drawables i :props char] value)))

(defn remove-drawable-prop [entity name char]
  (let [drawable (get-entity-drawable entity name)
        i (index-of (:drawables (entity-by-id (:uid entity))))]
     (swap! entities update-in [(:uid entity) :drawables i :props ] dissoc char)))

(defn get-entity-drawable [entity name]
  (let [drawables (:drawables (entity-by-id (:uid entity)))]
    (first (filter #(= name (:name %)) drawables))))

(defn get-entity-drawables [entity name]
  (let [drawables (:drawables (entity-by-id (:uid entity)))]
    (filter #(= name (:name %)) drawables)))

(defmulti register-event-handler (fn [class type drawable event handler] class))

(defmethod register-event-handler :entity [class type drawable event handler]
  (when (nil? (get-in @entity-events [type drawable event]))
    (swap! entity-events assoc-in [type drawable event] handler)))

(defmethod register-event-handler :attribute [class type drawable event handler]
  (when (nil? (get-in @attribute-events [type drawable event]))
    (swap! attribute-events assoc-in [type drawable event] handler)))


(defn add-entity-drawable [entity & drawables]
  (doseq [drawable_m (vec drawables)]
    (let [drawable (Drawable. (:name drawable_m)
                              (:type drawable_m)
                              (:src  drawable_m)
                              (:props drawable_m))]
      (make-js-property (:src drawable) ID  (:uid entity))
      (make-js-property (:src drawable) PART_ID (:name drawable))
      (let [drawables (conj (:drawables (entity-by-id (:uid entity))) drawable)]
         (swap! entities assoc-in [(:uid entity) :drawables] drawables)))))

(defn remove-entity-drawable [entity drawable-name]
  (let [drawables (:drawables (entity-by-id (:uid entity)))
        idx (index-of drawables (get-entity-drawable entity drawable-name))
        updated (into [] (concat (subvec drawables 0 idx)
                                 (subvec drawables (inc idx))))]
     (swap! entities assoc-in [(:uid entity) :drawables] updated)))

(defn get-attribute [name]
  (get @attributes name))

(defn is-attribute [name]
  (not (nil? (get-attribute name))))

(defn add-attribute [attribute]
  (when-not (is-attribute (:name attribute))
    (swap! attributes assoc-in [(:name attribute)] attribute)))

; Should not use drawables in argument list. Istead pass drawable factory-like function into the attribute definition.
(defn create-attribute-value [attribute value img drawables]
  (let [drawables_ (into {} (mapv (fn [d] {(:name d) (Drawable. (:name d) (:type d) (:src d) (:props d))}) drawables))]
    (AttributeValue. (str (random-uuid)) attribute value img drawables_)))

(defn add-entity-attribute-value [entity & attributes]
  (doseq [attribute-value (vec attributes)]
    (let [entity-fetch (entity-by-id (:uid entity))
          existing-cardinality (count (filter #(= (-> % :attribute :name) (-> attribute-value :attribute :name)) (:attributes entity-fetch)))
          cardinality (:cardinality (:attribute attribute-value))]
      (if (> cardinality existing-cardinality)
        (do
          (doseq [drawable (vals (:drawables attribute-value))]
            (make-js-property (:src drawable) ID  (:uid entity))
            (make-js-property (:src drawable) ATTR_ID (:id attribute-value))
            (make-js-property (:src drawable) PART_ID (:name drawable)))
          (let [attributes (conj (:attributes entity-fetch) attribute-value)
                sorted (sort-by #(:index (:attribute %)) attributes)]
             (swap! entities assoc-in [(:uid entity) :attributes] sorted)))
        (throw (js/Error. "Trying to add more attribute values than specified attribute definition cardinality!"))))))

(defn get-attribute-value-drawable [attribute-value drawable-name]
  (get (:drawables attribute-value) drawable-name))

(defn get-attribute-value-drawable-source [attribute-value drawable-name]
  (:src (get-attribute-value-drawable attribute-value drawable-name)))
