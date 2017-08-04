(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.general :as utils :refer [make-js-property]]))


(defonce ^:private ID "refId")
(defonce ^:private PART_ID "refPartId")
(defonce ^:private ATTR_ID "refAttrId")
(defonce ^:private ATTR_DRAWABLE_ID "refAttrDrawableId")

(defonce entities (atom {}))

(defonce paged-entities (atom {}))

(defonce events (atom {}))

(declare js-obj-id)

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defrecord Attribute [name cardinality weight domain create sync])

(defrecord AttributeValue [id attribute value img drawables])

(defrecord EntityDrawable [name type src rels])

(defprotocol IEntity
  (add-attribute [this attribute])
  (connect-to [this entity]))

(defrecord Entity [uid
                   type
                   drawables
                   attributes
                   relationships]

  IEntity
  (add-attribute [this attribute])
  (connect-to [this entity]))

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type drawables]
   (let [uid (str (random-uuid))
         entity (Entity. uid type drawables [] [])]
      (doseq [drawable drawables]
        (make-js-property (:src drawable) ID  (:uid entity))
        (make-js-property (:src drawable) PART_ID (:name drawable)))
      (swap! entities assoc uid entity)
      entity)))

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

(defn update-drawable-rel [entity name char value]
  (let [drawable (get-entity-drawable entity name)
        i (index-of (:drawables (entity-by-id (:uid entity))) drawable)]
     (swap! entities assoc-in [(:uid entity) :drawables i :rels char] value)))

(defn remove-drawable-rel [entity name char]
  (let [drawable (get-entity-drawable entity name)
        i (index-of (:drawables (entity-by-id (:uid entity))))]
     (swap! entities update-in [(:uid entity) :drawables i :rels ] dissoc char)))

(defn get-entity-drawable [entity name]
  (let [drawables (:drawables (entity-by-id (:uid entity)))]
    (first (filter #(= name (:name %)) drawables))))

(defn get-entity-drawables [entity name]
  (let [drawables (:drawables (entity-by-id (:uid entity)))]
    (filter #(= name (:name %)) drawables)))

(defn- handle-event [entity-type drawable event handler]
  (when (nil? (get-in @events [entity-type drawable event]))
    (swap! events assoc-in [entity-type drawable event] handler)))

(defn add-entity-drawable [entity & drawables]
  (doseq [drawable_m (vec drawables)]
    (let [drawable (EntityDrawable. (:name drawable_m)
                                    (:type drawable_m)
                                    (:src  drawable_m)
                                    (:rels drawable_m))]
      (make-js-property (:src drawable) ID  (:uid entity))
      (make-js-property (:src drawable) PART_ID (:name drawable))
      (let [drawables (conj (:drawables (entity-by-id (:uid entity))) drawable)]
         (swap! entities assoc-in [(:uid entity) :drawables] drawables)))
    (when (not (nil? (:behaviours drawable_m)))
      (doseq [event-type (keys (:behaviours drawable_m))]
        (handle-event (:type entity)
                      (:name drawable_m)
                      event-type
                      (get (:behaviours drawable_m) event-type))))))

(defn remove-entity-drawable [entity drawable-name]
  (let [drawables (:drawables (entity-by-id (:uid entity)))
        idx (index-of drawables (get-entity-drawable entity drawable-name))
        updated (into [] (concat (subvec drawables 0 idx)
                                 (subvec drawables (inc idx))))]
     (swap! entities assoc-in [(:uid entity) :drawables] updated)))

(defmulti create-entity-for-type (fn [type data-obj] type))

(defn create-attribute-value [attribute value img drawables]
  (AttributeValue. (str (random-uuid)) attribute value img drawables))

(defn add-entity-attribute [entity & attributes]
  (doseq [attribute-value (vec attributes)]
    (doseq [drawable (:drawables attribute-value)]
      (make-js-property (:src drawable) ID  (:uid entity))
      (make-js-property (:src drawable) ATTR_ID (:id attribute-value))
      (make-js-property (:src drawable) ATTR_DRAWABLE_ID (:name drawable)))
    (let [attributes (conj (:attributes (entity-by-id (:uid entity))) attribute-value)]
       (swap! entities assoc-in [(:uid entity) :attributes] attributes))))
