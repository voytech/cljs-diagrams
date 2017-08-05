(ns impl.attributes
 (:require [core.entities :as e]
           [core.project :as p])
 (:require-macros [core.macros :refer [defattribute]]))

(defattribute name data options
  {:cardinality 1
   :index 0
   :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))}
  {:width 100 :height 50} 
  [{:name "name"
    :type :title
    :src  (js/fabric.Text. "Name" options)}
   {:name "value"
    :type :value
    :src (js/fabric.Text. (:value data) (merge options {:left (+ 50 (:left options))}))}])

(defattribute description data options
  {:cardinality 1
   :index 1
   :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))}
  {:width 100 :height 50}
  [{:name "desc"
    :type :description
    :src  (js/fabric.Text. "Description" options)}
   {:name "value"
    :type :value
    :src (js/fabric.Text. (:value data) (merge options {:left (+ 50 (:left options))}))}])
