(ns core.tools.tool
  (:require [cljs-uuid-utils.core :as u]))

(def tools (atom {}))

(defn uuid []
  (-> (u/make-random-uuid) (u/uuid-string)))


(defrecord Tool [uid
                 name
                 desc
                 type
                 icon
                 func-ctor])

(defn create-tool
  ([name desc type icon func-ctor]
     (let [tool (Tool. (uuid) name desc type icon func-ctor)]
       (swap! tools assoc (:uid tool) tool)
       tool))
  ([name type func-ctor]
     (create-tool name "?" type nil func-ctor))
  ([name desc type func-ctor]
     (create-tool name desc type nil func-ctor)))

(defn by-name [name]
  (get @tools name))

(defn by-id [id]
  (get @tools id))
