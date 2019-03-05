(ns cljs-diagrams.core.edn
  (:require [cljs-diagrams.core.eventbus :as bus]
            [clojure.string :as str]
            [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.entities :as e]
            [cljs-diagrams.core.layouts :as l]
            [cljs-diagrams.core.state :as state]))


(defn export-component [component]
  (let [derecord (into {} component)]
    (-> derecord
        (assoc  :model (-> component :model deref))
        (dissoc :initializer)
        (dissoc :property-change-callback))))

(defn export-entity [entity]
  {:uid  (:uid entity)
   :type (:type entity)
   :bbox (:bbox entity)
   :tags (:tags entity)
   :relationships (:relationships entity)
   :component-properties (:component-properties entity)
   :layouts (-> entity :layouts vals)
   :components (mapv export-component (-> entity :components vals))})


(defn load-component [app-state entity-type component]
  (console.log (clj->js (d/model-callback app-state entity-type (:name component))))
  (-> component
      (assoc :model (volatile! (:model component)))
      (assoc :property-change-callback (d/model-callback app-state entity-type (:name component)))))

(defn load-entity [app-state data]
  (let [{:keys [type tags bbox component-properties components layouts]} data]
    (-> data
        (assoc :layouts (apply merge (mapv (fn [l] {(:name l) l}) layouts)))
        (assoc :components (apply merge (mapv (fn [c] {(:name c) (load-component app-state type c)}) components))))))