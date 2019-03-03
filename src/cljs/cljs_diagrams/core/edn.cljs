(ns cljs-diagrams.core.edn
  (:require [cljs-diagrams.core.eventbus :as bus]
            [clojure.string :as str]
            [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.entities :as e]
            [cljs-diagrams.core.layouts :as l]
            [cljs-diagrams.core.state :as state]))


(defn normalize-component [component]
  (let [derecord (into {} component)]
    (-> derecord
        (assoc  :model (-> component :model deref))
        (dissoc :layout-attributes)
        (dissoc :initializer)
        (dissoc :property-change-callback))))

(defn normalize-layout [layout]
  (-> (into {} layout)
      (assoc :position (into {} (:position layout)))
      (assoc :size (into {} (:size layout)))
      (assoc :margins (into {} (:margins layout)))))

(defn normalize-entity [entity]
  {:uid  (:uid entity)
   :type (:type entity)
   :bbox (:bbox entity)
   :tags (:tags entity)
   :relationships (:relationships entity)
   :component-properties (:component-properties entity)
   :layouts (mapv normalize-layout (-> entity :layouts vals))
   :components (mapv normalize-component (-> entity :components vals))})

(defn call-component [app-state entity args-map]
  (e/add-entity-component app-state entity args-map))

(defn recreate-components [entity app-state components]
  (reduce #(call-component app-state %1 %2) entity components))

(defn recreate-layouts [entity app-state layouts]
  (reduce #(e/add-layout app-state %1 (l/layout %2)) entity layouts))

(defn recreate-entity [app-state data]
  (let [{:keys [type tags bbox component-properties]} data]
    (-> (e/create-entity app-state type tags bbox component-properties)
        (recreate-layouts app-state (:layouts data))
        (recreate-components app-state (:components data)))))
