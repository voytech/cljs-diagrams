(ns core.api.flows
  (:require [tailrecursion.javelin :as j :refer [cell]])
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(defn flow [cel precond action]
  (cell= (when (precond cel) (action cel))))
