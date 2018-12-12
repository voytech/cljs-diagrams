(ns impl.behaviours.definitions
  (:require [core.behaviours :as b]
            [core.eventbus :as bus]
            [core.options :as o]
            [core.entities :as e]
            [core.events :as ev]
            [core.components :as d]
            [impl.behaviours.standard-api :as std]
            [impl.behaviours.manhattan :as m]
            [impl.components :as c]
            [impl.features.default :as f])
  (:require-macros [core.macros :refer [defbehaviour]]))

(defbehaviour moving-node-entity
              "Default Entity Moving"
              :node-moving
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "move")
              (fn [e]
                 (let [event (:context e)]
                    ((std/moving-entity) event)
                    nil)))

(defbehaviour moving-connector-entity-by
              "Default Relation Link Moving By"
              :moving-by
              [f/is-association-entity]
              (b/build-event-name "moveby")
              (fn [e]
                (let [event (:context e)
                      entity (:entity event)
                      app-state (-> event :app-state)
                      start (first (e/get-related-entities app-state entity :start))
                      end (first (e/get-related-entities app-state entity :end))
                      enriched (merge event {:start start :end end})]
                  (m/on-source-entity-event enriched)
                  nil)))

(defbehaviour moving-connector-endpoints
              "Connector's endpoints moving [Manhattan]"
              :connector-endpoint-moving
              [f/is-association-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ] "move")
              (fn [e]
                (let [event (:context e)]
                  (m/on-endpoint-event event)
                  ((std/intersects? "body" (fn [src trg] (std/toggle-controls (:entity trg) true))
                                           (fn [src trg] (std/toggle-controls (:entity trg) false))) (:context e))
                  nil)))

(defbehaviour make-relation
              "Connect Two Entities"
              :make-relation
              [f/is-association-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ] "mouse-up")
              (fn [e]
                (let [event (:context e)
                      app-state (-> event :app-state)]
                  ((std/intersects-controls? (fn [src trg]
                                               (let [ctype (-> event :component :type)
                                                     end-type (cond
                                                                (= ::c/endpoint ctype) {:type "end" :f std/position-endpoint}
                                                                (= ::c/startpoint ctype) {:type  "start" :f std/position-startpoint})]
                                                (e/connect-entities app-state (:entity src) (:entity trg) (keyword (:type end-type)))
                                                (std/toggle-controls (:entity trg) false)
                                                ((:f end-type) app-state (:entity src) (d/get-left (:component trg)) (d/get-top (:component trg)))))) (:context e))
                  (std/relations-validate app-state (-> event :entity))
                  (m/on-endpoint-event event)
                  nil)))

(defbehaviour hovering-entity
              "Default Entity Hovering"
              :hovering
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ::c/entity-shape] "focus")
              (fn [e]
                (let [event (:context e)]
                  ((std/highlight true o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  nil)))

(defbehaviour leaving-entity
              "Default Entity Leave"
              :leaving
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ::c/entity-shape] "blur")
              (fn [e]
                (let [event (:context e)]
                  ((std/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  nil)))

(defbehaviour show-all-entity-controls
              "Default Show Controls"
              :controls-show
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "focus")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-controls (:entity event) true)
                  nil)))

(defbehaviour hide-all-entity-controls
              "Default Hide Controls"
              :controls-hide
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-controls (:entity event) false)
                  nil)))

(defbehaviour show-entity-control
              "Default Show Control"
              :control-show
              [f/has-controls]
              (b/build-event-name [::c/control] "focus")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-control  (-> event :component) true)
                  nil)))

(defbehaviour hide-entity-control
              "Default Hide Control"
              :control-hide
              [f/has-controls]
              (b/build-event-name [::c/control] "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-control (-> event :component) false)
                  nil)))

(defbehaviour resize-entity
              "Resize Entity"
              :entity-resize
              [f/has-controls]
              (b/build-event-name [::c/control] "move")
              (fn [e]
                (let [event (:context e)]
                  (std/resize-with-control (:app-state event)
                                           (:entity event)
                                           (:component event)
                                           (:movement-x event)
                                           (:movement-y event))
                  nil)))
