(ns core.entities.entity
  (:require [data.js-cell :as jc]
            [tailrecursion.javelin :refer [cell destroy-cell!]]
            [cljs-uuid-utils.core :as u])
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]))


(def ^:private ID "refId")

(def entities (atom {}))

(declare js-obj-id)

(defn uuid []
  (-> u/make-random-uuid u/uuid-string)
  ;(.uuid js/Math 10 16)
  )

(defprotocol IEntity
  (data [this])
  (refresh [this])
  (prop-get  [this property])
  (prop-set  [this property val]))

(defrecord Entity [uid
                   type
                   src
                   propcel
                   event-handlers]
  IEntity
  (data [this]
    (jc/data propcel)
  )
  (prop-get [this property]
    (jc/get propcel property);(cell= (get-in propcel [(keyword property)])) ;;
  )
  (prop-set [this property val]
    (println (str propcel property val))
    (jc/set propcel property val);(assoc-in propcel [(keyword property)] val) ;;
  )
  (refresh [this] ;;
     (jc/refresh propcel)
  )
)



(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type src event-handlers]
     (let [uid (or (js-obj-id src) (uuid)) ; if we are re-creating entity after loading page - there is already identifier.
           propc (jc/js-cell src) ;(reactive-properties src)
           entity  (Entity. uid type src propc event-handlers)]
       (.defineProperty js/Object src ID  (js-obj "value" (:uid entity)
                                                  "writable" true
                                                  "configurable" true
                                                  "enumerable" true))
       (when (not (nil? event-handlers))
         (doseq [key (keys event-handlers)]
           (println (str "registering event handler : " key))
           (when (not (or (= key "collide") (= key "collide-end")))
             (.on src key (get event-handlers key)))))
       (println (str "Created entity " type " with id " (.-refId (:src entity))))
       (swap! entities assoc uid entity)
       entity))
  ([type src] (create-entity type src {}))
)

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
