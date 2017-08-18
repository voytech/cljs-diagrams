(ns impl.standard-attributes
 (:require [core.entities :as e]
           [core.behaviours :as behaviours]
           [core.project :as p]
           [impl.drawables :as d]
           [core.options :as o])

 (:require-macros [core.macros :refer [defattribute with-components value]]))

(def highlight-hovering
  {"mouse:over" (behaviours/highlight true  (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7}))
   "mouse:out"  (behaviours/highlight false (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7}))})

(defattribute name
  (with-definition
    {:cardinality 1
     :index 0
     :sync (fn [attr-value])})
  (with-components data options
    [{:name "value"
      :type :value
      :drawable (d/text {:text data})}])
  (with-behaviours
    {:value highlight-hovering}))

(defattribute description
  (with-definition
    {:cardinality 1
     :index 1
     :sync (fn [attr-value])})
  (with-components data options
    [{:name "desc"
      :type :description
      :drawable  (d/text {:text "Description"})}
     {:name "value"
      :type :value
      :drawable (d/text {:text data :left 60})}])
  (with-behaviours
    {:value highlight-hovering}))

(defn status-components [status])


(defattribute state
  (with-definition
    {:cardinality 1
     :index 4})
  (with-domain
     [(value :open
        (with-components data options
          [{:name "value-open"
            :type :value
            :drawable (d/text {:text "OPEN"})}]))
      (value :progress
        (with-components data options
          [{:name "value-progress"
            :type :value
            :drawable (d/text {:text "PROGRESS"})}]))
      (value :closed
        (with-components data options
          [{:name "value-closed"
            :type :value
            :drawable (d/text {:text "CLOSED"})}]))]))
