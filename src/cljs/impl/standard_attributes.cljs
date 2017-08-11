(ns impl.standard-attributes
 (:require [core.entities :as e]
           [core.behaviours :as behaviours]
           [core.project :as p]
           [core.options :as o])

 (:require-macros [core.macros :refer [defattribute with-drawables value]]))

(def highlight-hovering
  {"mouse:over" (behaviours/highlight true  (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7}))
   "mouse:out"  (behaviours/highlight false (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7}))})

(defattribute name data options
  (with-definition
    {:cardinality 1
     :index 0
     :sync (fn [attr-value] (.setText (e/get-attribute-value-drawable-source attr-value "value") (:value attr-value)))})
  (with-drawables
    [{:name "value"
      :type :value
      :src (js/fabric.Text. data (clj->js (merge o/LOCKED o/TEXT_HEADER_DEFAULTS)))}])
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
      :src (js/fabric.Text. data (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS {:left 60})))}])
  (with-behaviours
    {:value highlight-hovering}))

(defn status-components [status])


(defattribute state data options
  (with-definition
    {:cardinality 1
     :index 4})
  (with-domain
     [(value :open
        (with-drawables
          [{:name "value-open"
            :type :value
            :src (js/fabric.Text. "OPEN" (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS)))}]))
      (value :progress
        (with-drawables
          [{:name "value-progress"
            :type :value
            :src (js/fabric.Text. "PROGRESS" (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS)))}]))
      (value :closed
        (with-drawables
          [{:name "value-closed"
            :type :value
            :src (js/fabric.Text. "CLOSED" (clj->js (merge o/LOCKED o/TEXT_NORMAL_DEFAULTS)))}]))]))
