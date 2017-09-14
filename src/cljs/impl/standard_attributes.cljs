(ns impl.standard-attributes
 (:require [core.entities :as e]
           [impl.behaviours.standard-api :as behaviours]
           [core.project :as p]
           [impl.drawables :as d]
           [impl.components :as c]
           [core.options :as o])

 (:require-macros [core.macros :refer [defattribute with-components value]]))

(def highlight-hovering
  {"mouse:over" (behaviours/highlight true  (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7}))
   "mouse:out"  (behaviours/highlight false (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7}))})

(defattribute name
  (with-definition
    {:cardinality 1
     :index 0})
  (with-components data options
    [(c/value "value" {:text data})])
  (with-behaviours
    {:value highlight-hovering}))

(defattribute description
  (with-definition
    {:cardinality 1
     :index 1})
  (with-components data options
    [(c/description "desc" {:text "Description"})
     (c/value "value" {:text data :left 60})])
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
          [(c/value "value-open" {:text "OPEN"})]))
      (value :progress
        (with-components data options
          [(c/value "value-progress" {:text "PROGRESS"})]))
      (value :closed
        (with-components data options
          [(c/value "value-closed" {:text "CLOSED"})]))]))
