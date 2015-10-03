(ns core.api.rpc-wrap
  (:require [tailrecursion.javelin :as j :refer [cell]]))

(def ^:private cells (atom {}))

(defn- do-cell [method result-type]
  (when-not (get-in @cells [method :success])
    (swap! cells assoc-in [method :success] (cell {})))
  (get-in @cells [method :success]))

(defn on-success [method]
  (do-cell method :success))

(defn on-error [method]
  (do-cell method :error))
