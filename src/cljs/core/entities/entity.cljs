(ns core.entities.entity)

(def ^:private ID "refId")

(def entities (atom {}))

(defn uuid []
  (.uuid js/Math 10 16))

(defrecord Entity [uid
                   type
                   src
                   event-handlers
                   collide-func
                   collide-func-end])

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type src event-handlers collide-func collide-func-end]
     (let [uid (uuid)
           entity (Entity. uid type src event-handlers collide-func collide-func-end)]
       (.defineProperty js/Object src ID  (js-obj "value" (:uid entity)
                                                  "writable" true
                                                  "configurable" true
                                                  "enumerable" true))
       (when (not (nil? event-handlers))
         (doseq [key (keys event-handlers)]
           (println (str "registering event handler : " key))
           (.on src key (get event-handlers key))))
       (println (str "Created entity " type " with id " (.-refId (:src entity))))
       (swap! entities assoc uid entity)
       entity))
  ([type src] (create-entity type src nil (fn [src trg]) (fn [src trg])))
  ([type src event-handlers]
     (create-entity type src event-handlers (fn [src trg]) (fn [src trg])))
  )

(defn entity-by-id [id]
  (get @entities id))

(defn js-obj-id [src]
  (.-refId src))

(defn entity-from-src [src]
  (-> src
      js-obj-id
      entity-by-id))
