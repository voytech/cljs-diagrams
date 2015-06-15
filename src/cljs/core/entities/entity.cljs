(ns core.entities.entity
  (:require [data.js-cell :as jc]
            [tailrecursion.javelin :refer [cell destroy-cell!]])
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]))


(def ^:private ID "refId")

(def entities (atom {}))


(defn uuid []
  (.uuid js/Math 10 16))

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

;; (defn- properties [jsobj]
;;   (let [props (atom [])]
;;     (goog.object/forEach jsobj
;;                          (fn [val key obj]
;;                            (when (not (= (type val) js/Function))
;;                              (swap! props conj key))))
;;     @props))


;; (defn- reactive-properties [obj]
;;   (let [propcel (cell {})]
;;     (doseq [property (properties obj)]
;;       (let [val (goog.object/get obj property)]
;;         (.defineProperty js/Object obj property (js-obj
;;                                                  "set" #(swap! propcel assoc-in [(keyword property)] %)
;;                                                  "get" #(get-in @propcel [(keyword property)])))
;;         (goog.object/set obj property val))
;;        propcel
;;     ))
;; )


(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type src event-handlers]
     (let [uid (uuid)
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
