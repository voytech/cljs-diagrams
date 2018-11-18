(ns impl.standard-attributes
 (:require [core.entities :as e]
           [core.behaviours :as b]
           [impl.features.default :as f]
           [impl.behaviours.standard-api :as behaviours]
           [core.project :as p]
           [core.components :as d]
           [impl.components :as c]
           [core.behaviours :as b]
           [core.events :as ev]
           [core.options :as o]
           [impl.behaviours.editors :as ed])

 (:require-macros [core.macros :refer [defattribute with-components with-behaviours with-domain value]]))

(defattribute name
  (with-definition
    {:cardinality 1
     :index 0
     :sync ["value.text"]})
  (with-components data options
    [(c/text "value" {:text data})])
  (with-behaviours))

(defattribute description
  (with-definition
    {:cardinality 1
     :index 1
     :sync ["value.text"]})
  (with-components data options
    [(c/text "value" {:text data})])
  (with-behaviours))

(defattribute state
  (with-definition
    {:cardinality 1
     :index 4})
  (with-domain
     [(value :open
        (with-components data options
           [(c/text "value-open" {:text "[OPEN]" :border-color "green" :left 2 })]))
      (value :progress
        (with-components data options
           [(c/text "value-progress" {:text "[PROGRESS]" :border-color "blue" :left 2 })]))
      (value :closed
        (with-components data options
           [(c/text "value-closed" {:text "[CLOSED]" :border-color "black" :left 2 })]))]))

(b/add-behaviour 'attribute-hovering
                 "Default Attribute Hover"
                 :attribute-hovering
                 [f/is-text-attribute]
                 (b/build-event-name [::c/text] "focus")
                 (fn [e]
                   (let [event (:context e)]
                     ((behaviours/highlight true (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7})) event)
                     nil)))

(b/add-behaviour 'attribute-leaving
                 "Default Attribute Leave"
                 :attribute-leaving
                 [f/is-text-attribute]
                 (b/build-event-name [::c/text] "blur")
                 (fn [e]
                   (let [event (:context e)]
                     ((behaviours/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                     nil)))

(b/add-behaviour 'state-attribute-editing
                 "Attribute Edit"
                 :attribute-editing
                 [f/is-selection-attribute]
                 (b/build-event-name [:c/text] "activate")
                 (fn [e]
                   (let [event (:context e)]
                     (ed/domain-editor event)
                     nil)))

(b/add-behaviour 'text-attribute-editing
                "Attribute Edit"
                :attribute-editing
                [f/is-single-attribute]
                (b/build-event-name [:c/text] "activate")
                (fn [e]
                  (let [event (:context e)]
                    (ed/editor event)
                    nil)))
