(ns core.resources
 (:require [reagent.core :as reagent :refer [atom]]
           [core.tools :as t]))

(defonce resources (atom {}))

(defn add-resource [{:keys [name type content] :as res}]
  (swap! resources assoc type res))
