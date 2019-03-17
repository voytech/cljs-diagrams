(ns cljs-diagrams.core.edn
  (:require [cljs-diagrams.core.eventbus :as bus]
            [clojure.string :as str]
            [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.layouts :as l]
            [cljs-diagrams.core.state :as state]))


(defn export-shape [shape]
  (let [derecord (into {} shape)]
    (-> derecord
        (assoc  :model (-> shape :model deref))
        (dissoc :model-listener)
        (dissoc :initializer))))

(defn export-node [node]
  {:uid  (:uid node)
   :type (:type node)
   :bbox (:bbox node)
   :tags (:tags node)
   :relationships (:relationships node)
   :shapes-properties (:shapes-properties node)
   :layouts (-> node :layouts vals)
   :shapes (mapv export-shape (-> node :shapes vals))})

(defn load-shape [app-state node-type shape]
  (let [customizers (:model-customizers shape)]
    (-> shape
        (assoc :model (volatile! (:model shape)))
        (assoc :model-listener (d/customize-model app-state customizers)))))

(defn load-node [app-state data]
  (let [{:keys [type tags bbox shapes-properties shapes layouts]} data]
    (-> data
        (assoc :layouts (apply merge (mapv (fn [l] {(:name l) l}) layouts)))
        (assoc :shapes (apply merge (mapv (fn [c] {(:name c) (load-shape app-state type c)}) shapes))))))