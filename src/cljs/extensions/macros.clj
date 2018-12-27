(ns extensions.macros
  (:require [cljs.analyzer :as a]))

(defn resolve-namespace-name []
  (let [file-struct (-> a/*cljs-file* slurp read-string)]
    (name (second file-struct))))

(defmacro defresolver [name feature spec handler]
  (let [nsname (resolve-namespace-name)]
   `(defn ~name [app-state# entity#  data#]
      (extensions.data-resolvers/register app-state# (keyword ~nsname (name '~name)) ~feature ~spec ~handler)
      (extensions.data-resolvers/apply-data app-state# entity# data#))))
