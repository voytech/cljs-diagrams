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
            [impl.features.default :as f]))

(b/add-behaviour 'moving-node-entity
                 "Default Entity Moving"
                 :node-moving
                 [f/is-primary-entity]
                 (b/build-event-name [::c/main] "move")
                 (fn [e]
                   (let [event (:context e)]
                     ((std/moving-entity) event)
                     nil)))

(b/add-behaviour 'moving-connector-entity-by
                 "Default Relation Link Moving By"
                 :moving-by
                 [f/is-association-entity]
                 (b/build-event-name "moveby")
                 (fn [e]
                   (let [event (:context e)
                         entity (:entity event)
                         start (first (e/get-related-entities entity :start))
                         end (first (e/get-related-entities entity :end))
                         enriched (merge event {:start start :end end})]
                     (m/on-source-entity-event enriched)
                     nil)))

(b/add-behaviour 'moving-connector-endpoints
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

(b/add-behaviour 'make-relation
                "Connect Two Entities"
                :make-relation
                [f/is-association-entity]
                (b/build-event-name [::c/startpoint ::c/endpoint ] "mouse-up")
                (fn [e]
                  (let [event (:context e)]
                    ((std/intersects-controls? (fn [src trg]
                                                 (let [ctype (-> event :component :type)
                                                       end-type (cond
                                                                  (= ::c/endpoint ctype) {:type "end" :f std/position-endpoint}
                                                                  (= ::c/startpoint ctype) {:type  "start" :f std/position-startpoint})]
                                                  (e/connect-entities (:entity src) (:entity trg) (keyword (:type end-type)))
                                                  (std/toggle-controls (:entity trg) false)
                                                  ((:f end-type) (:entity src) (d/get-left (:component trg)) (d/get-top (:component trg)))))) (:context e))
                    (std/relations-validate (->> e :context :entity))
                    (m/on-endpoint-event event)
                    nil)))

(b/add-behaviour 'hovering-entity
                "Default Entity Hovering"
                :hovering
                [f/is-association-entity f/is-primary-entity]
                (b/build-event-name [::c/startpoint ::c/endpoint ::c/main] "focus")
                (fn [e]
                  (let [event (:context e)]
                    ((std/highlight true o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                    nil)))

(b/add-behaviour 'leaving-entity
                "Default Entity Leave"
                :leaving
                [f/is-association-entity f/is-primary-entity]
                (b/build-event-name [::c/startpoint ::c/endpoint ::c/main] "blur")
                (fn [e]
                  (let [event (:context e)]
                    ((std/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                    nil)))

(b/add-behaviour 'show-all-entity-controls
                "Default Show Controls"
                :controls-show
                [f/is-primary-entity]
                (b/build-event-name [::c/main] "focus")
                (fn [e]
                  (let [event (:context e)]
                    (std/toggle-controls (:entity event) true)
                    nil)))

(b/add-behaviour 'hide-all-entity-controls
                "Default Hide Controls"
                :controls-hide
                [f/is-primary-entity]
                (b/build-event-name [::c/main] "focus")
                (fn [e]
                  (let [event (:context e)]
                    (std/toggle-controls (:entity event) false)
                    nil)))

(b/add-behaviour 'show-entity-control
                "Default Show Control"
                :control-show
                [f/is-primary-entity]
                (b/build-event-name [::c/main] "focus")
                (fn [e]
                  (let [event (:context e)]
                    (std/toggle-control (:entity event) (-> event :component :name) true)
                    nil)))

(b/add-behaviour 'hide-entity-control
                "Default Hide Control"
                :control-hide
                [f/is-primary-entity]
                (b/build-event-name [::c/main] "blur")
                (fn [e]
                  (let [event (:context e)]
                    (std/toggle-control (:entity event) (-> event :component :name) false)
                    nil)))
