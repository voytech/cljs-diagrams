(ns impl.attributes
 (:require [core.entities :as e]
           [core.project :as p])
 (:require-macros [core.macros :refer [defattribute]]))

(defattribute name data options
  {:cardinality 1
   :index 0
   :sync (fn [value] (.setText (:src (:value (:drawables value))) (:value value)))}
  [{:name "name"
    :type :title
    :src  (js/fabric.Text. "Name" options)}
   {:name "value"
    :type :value
    :src (js/fabric.Text. (:text data) options)}])

(defattribute description data options
  {:cardinality 1
   :index 1
   :sync (fn [value] (.setText (:src (:value (:drawables value))) (:value value)))}
  [{:name "desc"
    :type :description
    :src  (js/fabric.Text. "Description" options)}
   {:name "value"
    :type :value
    :src (js/fabric.Text. (:text data) options)}])
