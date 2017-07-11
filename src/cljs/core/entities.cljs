(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]))


(defonce ^:private ID "refId")
(defonce ^:private PART_ID "refPartId")

(defonce entities (atom {}))
(defonce paged-entities (atom {}))

(declare properties)
(declare js-obj-id)

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defrecord Part [name src event-handlers])

(defprotocol IEntity
  (add-attribute [this attribute])
  (connect-to [this entity]))

(defrecord Entity [uid
                   type
                   parts
                   attributes
                   relationships]

  IEntity
  (add-attribute [this attribute])
  (connect-to [this entity]))

(defn properties [jsobj func]
 (let [props (atom [])]
    (goog.object/forEach jsobj
                         (fn [val key obj]
                           (when (not (= (type val) js/Function))
                             (when (not (nil? func)) (func key val))
                             (swap! props conj key))))
   @props))

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type parts]
   (let [uid (str (random-uuid))
         entity (Entity. uid type parts [] [])]
      (doseq [part parts]
        (.defineProperty js/Object (:src part) ID  (js-obj "value" (:uid entity)
                                                           "writable" true
                                                           "configurable" true
                                                           "enumerable" true))
        (.defineProperty js/Object (:src part) PART_ID  (js-obj "value" (:name part)
                                                                "writable" true
                                                                "configurable" true
                                                                "enumerable" true)))
      (swap! entities assoc uid entity)
      entity)))

(defn bind [entity page]
  (let [euids (page @paged-entities)
        uid (:uid entity)]
    (swap! paged-entities assoc-in [page] (if (nil? euids) #{uid} (conj euids uid)))))

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

(defn entity-part [entity partname]
  (let [parts (:parts entity)]
    (first (filter #(= partname (:name %)) parts))))

(def EMPTY (create-entity "empty" (js/Object.)))

(defmulti create-entity-for-type (fn [type data-obj] type))
