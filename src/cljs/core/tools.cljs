(ns core.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-uuid-utils.core :as u]))

(defonce tools (atom {}))

(defn uuid []
  (-> (u/make-random-uuid) (u/uuid-string)))

(defrecord Tool [uid name desc type icon ctor])

(defn create-tool
  ([name desc type icon ctor]
   (let [tool (Tool. (uuid) name desc type icon ctor)]
    (swap! tools assoc (:uid tool) tool)
    tool))
  ([name type ctor]
   (create-tool name "?" type nil ctor))
  ([name desc type ctor]
   (create-tool name desc type nil ctor)))

(defn invoke-tool [tool context]
  (let [ctor (:ctor tool)]
    ((ctor nil) context)))

(defn by-name [name]
  (get @tools name))

(defn by-id [id]
  (get @tools id))

(defn by-type [type]
  (filter #(= type (:type %)) (vals @tools)))
