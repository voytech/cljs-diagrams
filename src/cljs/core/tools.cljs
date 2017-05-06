(ns core.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-uuid-utils.core :as u]))

(defonce tools (atom {}))

(defn uuid []
  (-> (u/make-random-uuid) (u/uuid-string)))

(defrecord Tool [uid name desc type icon generator])

(defn create-tool
  ([name desc type icon generator]
   (let [tool (Tool. (uuid) name desc type icon generator)]
    (swap! tools assoc (:uid tool) tool)
    tool))
  ([name type generator]
   (create-tool name "?" type nil generator))
  ([name desc type generator]
   (create-tool name desc type nil generator)))

(defn by-name [name]
  (get @tools name))

(defn by-id [id]
  (get @tools id))

(defn by-type [type]
  (filter #(= type (:type %)) @tools))
