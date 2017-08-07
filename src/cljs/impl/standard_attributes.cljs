(ns impl.standard-attributes
 (:require [core.entities :as e]
           [core.project :as p])
 (:require-macros [core.macros :refer [defattribute]]))

(defattribute name data options
  {:cardinality 1
   :index 0
   :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))}
  [{:name "value"
    :type :value
    :src (js/fabric.Text. (:value data) (clj->js {:fontSize 14 :fontWeight "bold" :width 180 :textAlign "center"}))}])

(defattribute description data options
  {:cardinality 1
   :index 1
   :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))}
  [{:name "desc"
    :type :description
    :src  (js/fabric.Text. "Description" (clj->js {:fontSize 12}))}
   {:name "value"
    :type :value
    :src (js/fabric.Text. (:value data) (clj->js {:left 70 :fontSize 12}))}])
