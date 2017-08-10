(ns impl.standard-attributes
 (:require [core.entities :as e]
           [core.project :as p]
           [core.options :as o])

 (:require-macros [core.macros :refer [defattribute]]))

(defattribute name data options
  (with-definition
    {:cardinality 1
     :index 0
     :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))})
  (with-drawables
    [{:name "value"
      :type :value
      :src (js/fabric.Text. (:value data) (clj->js (merge o/LOCKED o/TEXT_HEADER_DEFAULTS)))}]))

(defattribute description data options
  (with-definition
    {:cardinality 1
     :index 1
     :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))})
  (with-drawables
    [{:name "desc"
      :type :description
      :src  (js/fabric.Text. "Description" (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS)))}
     {:name "value"
      :type :value
      :src (js/fabric.Text. (:value data) (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS {:left 70})))}]))

(defattribute field1TestAttr data options
  (with-definition
    {:cardinality 1
     :index 2
     :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))})
  (with-drawables
    [{:name "value"
      :type :value
      :src (js/fabric.Text. (:value data) (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS)))}]))

(defattribute field2TestAttr data options
  (with-definition
    {:cardinality 4
     :index 3
     :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))})
  (with-drawables
     [{:name "value"
       :type :value
       :src (js/fabric.Text. (:value data) (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS)))}]))
