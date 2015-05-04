(ns core.entities.entity)

(def ^:private ID "refId")

(def entities (atom {}))

(defn uuid []
  (.uuid js/Math 10 16))

(defrecord Entity [uid
                   type
                   src
                   on-attach
                   on-edit
                   on-detach
                   collide-func])

(defn create-entity
  "Creates editable entity backed by fabric.js object. Adds id identifier to original javascript object. "
  ([type src on-attach on-edit on-detach collide-func]
     (let [uid (uuid)
           entity (Entity. uid type src on-attach on-edit on-detach collide-func)]
       (.defineProperty js/Object src ID  (js-obj "value" (:uid entity)
                                                  "writable" true
                                                  "configurable" true
                                                  "enumerable" true))
       (println (str "Created entity " type " with id " (.-refId (:src entity))))
       (swap! entities assoc uid entity)
       entity))
  ([type src] (create-entity type src (fn [src trg])))
  ([type src on-attach on-edit on-detach]
     (create-entity type src on-attach on-edit on-detach (fn [src trg])))
  )

(defn entity-by-id [id]
  (get @entities id))

(defn js-obj-id [src]
  (.-refId src))
