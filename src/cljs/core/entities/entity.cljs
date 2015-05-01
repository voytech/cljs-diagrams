(ns core.entities.entity)

(def ^:private ID "refId")

(def entities (atom {}))

(defn uuid []
  (.uuid js/Math 10 16))

(defrecord Entity [uid type src collide-func])

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type src collide-func]
     (let [entity (Entity. (uuid) type src collide-func)]
       (.defineProperty js/Object src ID  (js-obj "value" uid
                                                  "writable" true
                                                  "configurable" true
                                                  "enumerable" true))
       (swap! entities assoc uid entity)
       entity))
  ([type src] (create-entity type src (fn [src trg]))))

(defn entity-by-id [id]
  (get @entities id))

(defn js-obj-id [src]
  (.-refId src))
