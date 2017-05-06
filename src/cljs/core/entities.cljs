(ns core.entities
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.general :as utils :refer [uuid]]))

(defonce ^:private ID "refId")

(defonce entities (atom {}))
(defonce paged-entities (atom {}))

(declare properties)
(declare js-obj-id)

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defprotocol IEntity
  (data [this])
  (refresh [this])
  (prop-get  [this property])
  (prop-set  [this property val]))

(defrecord Entity [uid
                   type
                   src
                   prop-state
                   event-handlers]
  IEntity
  (data [this]
    (jc/data propcel))

  (prop-get [this prop-keyword]
    (prop-keyword @prop-state))

  (prop-set [this property val]
    (swap! prop-state assoc-in [prop-keyword] val))

  (refresh [this] ;;
     (reset! prop-state @(manage-properties src))))

(defn properties [jsobj func]
 (let [props (atom [])]
    (goog.object/forEach jsobj
                         (fn [val key obj]
                           (when (not (= (type val) js/Function))
                             (when (not (nil? func)) (func key val))
                             (swap! props conj key))))
   @props))

(defn manage-properties [jsobj]
  (let [estate (atom {})]
    (properties jsobj (fn [key val] (swap! estate assoc key val)))
    estate))

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type src event-handlers]
   (let [uid (or (js-obj-id src) (uuid))
         state (manage-properties src)
         entity  (Entity. uid type src state event-handlers)]
     (.defineProperty js/Object src ID  (js-obj "value" (:uid entity)
                                                "writable" true
                                                "configurable" true
                                                "enumerable" true))
     (when (not (nil? event-handlers))
       (doseq [key (keys event-handlers)]
         (when (not (or (= key "collide") (= key "collide-end")))
           (.on src key (get event-handlers key)))))
     (swap! entities assoc uid entity)
     entity))
  ([type src ] (create-entity type src {})))

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

(defn get-entity-property [entity-id key]
  (let [entity (entity-by-id entity-id)]
    (key (data entity))))

(def EMPTY (create-entity "empty" (js/Object.)))

(defmulti create-entity-for-type (fn [type data-obj] type))
