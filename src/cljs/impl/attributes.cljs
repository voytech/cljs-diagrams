(ns impl.attributes
 (:require [core.entities :as e]
           [core.project :as p])
 (:require-macros [core.macros :refer [defattribute]]))

(defattribute name data options
  {:cardinality 1
   :index 0
   :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))}
  [{:name "name"
    :type :title
    :src  (js/fabric.Text. "Name" options)}
   {:name "value"
    :type :value
    :src (js/fabric.Text. (:value data) options)}])

(defattribute description data options
  {:cardinality 1
   :index 1
   :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))}
  [{:name "desc"
    :type :description
    :src  (js/fabric.Text. "Description" options)}
   {:name "value"
    :type :value
    :src (js/fabric.Text. (:value data) options)}])
