(ns core.tools
  (:require [reagent.core :as reagent :refer [atom]]))


(defonce tools (atom {}))

(defrecord Tool [uid name desc type icon ctor])

(defn create-tool
  ([name desc type icon ctor]
   (let [tool (Tool. (str (random-uuid)) name desc type icon ctor)]
    (swap! tools assoc (:uid tool) tool)
    tool))
  ([name type ctor]
   (create-tool name "?" type nil ctor))
  ([name desc type ctor]
   (create-tool name desc type nil ctor)))

(defn invoke-tool [tool entities context]
  (let [ctor (:ctor tool)]
    (ctor entities context)))

(defn by-name [name]
  (get @tools name))

(defn by-id [id]
  (get @tools id))

(defn by-type [type]
  (filter #(= type (:type %)) (vals @tools)))

(defn ctor
  ([entity data]
   (fn [entities context]
     (entity entities data context)))
  ([entity]
   (fn [entities context]
     (entity entities nil context))))
