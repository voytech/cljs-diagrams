(ns impl.standard-attributes
 (:require [core.entities :as e]
           [impl.behaviours.standard-api :as behaviours]
           [core.project :as p]
           [core.drawables :as cd]
           [impl.drawables :as d]
           [impl.components :as c]
           [core.behaviours :as b]
           [core.events :as ev]
           [core.options :as o]
           [impl.behaviours.editors :as ed])

 (:require-macros [core.macros :refer [defattribute defbehaviour having-all having-strict make-event validate bind-to -- with-components value invalid-when]]))

(defattribute name
  (with-definition
    {:cardinality 1
     :index 0
     :sync (fn [av]
             (cd/setp (e/get-attribute-value-drawable av "value") :text (:value av)))})

  (with-components data options
    [(c/value "value" {:text data})])
  (with-behaviours))

(defattribute description
  (with-definition
    {:cardinality 1
     :index 1
     :sync (fn [av]
             (cd/setp (e/get-attribute-value-drawable av "value") :text (:value av)))})
  (with-components data options
    [(c/description "desc" {:text "Description"})
     (c/value "value" {:text data :left 60})])
  (with-behaviours))

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


(defbehaviour attribute-hovering
              "Default Attribute Hover" :attribute-hovering
              (validate
                (-- (having-all ::c/value)
                    (bind-to ::c/value))
                (fn [target behaviour result]
                  (ev/loose-event-name nil (-> target :attribute :name) result "mousemove")))
              (fn [e]
                (let [event (:context @e)]
                  ((behaviours/highlight true (merge o/DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "blue" :normal-width 0.5 :highlight-width 0.7})) event)
                  nil)))

(defbehaviour leaving-attribute
            "Default Entity Leave" :leaving
            (validate
              (-- (having-all ::c/value)
                  (bind-to ::c/value))
              (fn [target behaviour result]
                (ev/loose-event-name nil (-> target :attribute :name) result "mouseout")))
            (fn [e]
              (let [event (:context @e)]
                ((behaviours/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                nil)))

(defbehaviour state-attribute-editing
              "Attribute Edit" :attribute-editing
              (fn [target this]
                (let [attribute (:attribute target)]
                  (when-let [domain (:domain attribute)] ;; needs to find a way how to obtain a component name for domain entry.
                    (ev/loose-event-name nil (-> attribute :name) ::c/value "mousepointclick"))))
              (fn [e]
                (let [event (:context @e)]
                  (ed/domain-editor event)
                  nil)))

(defbehaviour text-attribute-editing
              "Attribute Edit" :attribute-editing
              (validate
                (-- (having-all ::c/value)
                    (bind-to ::c/value))
                (-- (invalid-when #(< 0 (count (-> % :attribute :domain)))))
                (fn [target behaviour result]
                  (ev/loose-event-name nil (-> target :attribute :name) result "mousepointclick")))
              (fn [e]
                (let [event (:context @e)]
                  (ed/editor event)
                  nil)))
