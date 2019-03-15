(ns cljs-diagrams.extensions.data-resolvers
  (:require [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.state :as state]))

(defrecord Resolver [name feature spec resolver])

(defn register [app-state name feature spec resolver]
  (when (nil? (seq (filter #(= (:name %) name) (state/get-in-extensions-state app-state [:data-resolvers]))))
    (let [resolvers (-> (or (state/get-in-extensions-state app-state [:data-resolvers]) [])
                        (conj (Resolver. name feature spec resolver)))]
      (state/assoc-extensions-state app-state [:data-resolvers] resolvers))))

(defn apply-data [app-state node data]
  (let [resolvers (state/get-in-extensions-state app-state [:data-resolvers])
        valid-resolvers (filterv #(and (spec/valid? (:spec %) data)
                                       ((:feature %) node))
                                 resolvers)]
    (doseq [resolver valid-resolvers]
      ((:resolver resolver) app-state node data))))
