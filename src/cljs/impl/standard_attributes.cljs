(ns impl.standard-attributes
 (:require [core.entities :as e]
           [core.behaviours :as behaviours]
           [core.project :as p]
           [core.options :as o])

 (:require-macros [core.macros :refer [defattribute]]))

(def highlight-hovering
  {"mouse:over" (behaviours/highlight true  (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "green" :normal-width 0.5 :highlight-width 0.7}))
   "mouse:out"  (behaviours/highlight false (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "green" :normal-width 0.5 :highlight-width 0.7}))})

(defattribute name data options
  (with-definition
    {:cardinality 1
     :index 0
     :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))})
  (with-drawables
    [{:name "value"
      :type :value
      :src (js/fabric.Text. (:value data) (clj->js (merge o/LOCKED o/TEXT_HEADER_DEFAULTS)))}])
  (with-behaviours
    {:value highlight-hovering}))

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
      :src (js/fabric.Text. (:value data) (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS {:left 60})))}])
  (with-behaviours
    {:value highlight-hovering}))

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
