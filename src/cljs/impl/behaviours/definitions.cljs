(ns impl.behaviours.definitions
  (:require [core.behaviours :as b]
            [core.eventbus :as bus]
            [core.options :as o]
            [core.entities :as e]
            [core.events :as ev]
            [core.components :as d]
            [core.behaviour-api :as api]
            [core.selection :as s]
            [impl.behaviours.behaviour-api :as std]
            [impl.behaviours.manhattan :as m]
            [impl.components :as c]
            [impl.features.default :as f])
  (:require-macros [core.macros :refer [defbehaviour]]))

(defbehaviour moving-shape-entity
              "Shapeful Entity Moving"
              :rigid-entity-moving
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "move")
              (fn [e]
                 (let [{:keys [app-state component entity movement-x movement-y]} (:context e)]
                   (api/move-entity app-state entity movement-x movement-y)
                   nil)))

(defbehaviour moving-association-entity-by
              "Moving Association Entity By"
              :moving-by
              [f/is-association-entity]
              (b/build-event-name "moveby")
              (fn [e]
                (let [event (:context e)
                      entity (:entity event)
                      app-state (-> event :app-state)]
                  (m/refresh-manhattan-layout app-state entity)
                  nil)))

(defbehaviour moving-primary-entity-by
              "Moving Primary Entity Entity By"
              :moving-by
              [f/is-primary-entity]
              (b/build-event-name "moveby")
              (fn [e]
                 (let [{:keys [app-state entity movement-x movement-y]} (:context e)]
                   (api/move-entity app-state entity movement-x movement-y)
                   nil)))

(defbehaviour select-shape-entity
              "Select object"
              :select
              [f/is-primary-entity]
              (b/build-event-name [::c/edit] "activate")
              (fn [e]
                 (let [{:keys [app-state entity]} (:context e)]
                    (s/select app-state entity)
                    (bus/fire app-state "entity.selected" {:selection entity})
                    nil)))

(defbehaviour remove-entity
              "Remove object"
              :remove
              [f/is-primary-entity]
              (b/build-event-name [::c/remove] "activate")
              (fn [e]
                 (let [{:keys [app-state entity]} (:context e)]
                    (e/remove-entity app-state entity)
                    nil)))

(defbehaviour moving-association-endpoints
              "Association's endpoints moving [Manhattan]"
              :association-endpoint-moving
              [f/is-association-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ] "move")
              (fn [e]
                (let [event (:context e)
                      {:keys [app-state entity component movement-x movement-y]} event
                      end-type (if (= ::c/startpoint (:type component)) :start :end)]
                  (m/endpoint-move app-state entity end-type movement-x movement-y)
                  (api/collides? app-state
                                 component
                                 f/has-controls
                                 (fn [src trg] (std/toggle-controls (:entity trg) true))
                                 (fn [src]))
                  nil)))

(defbehaviour make-relation
              "Connect Two Entities"
              :make-relation
              [f/is-association-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ] "mouse-up")
              (fn [e]
                (let [{:keys [app-state component entity] :as event} (:context e)
                      ctype (:type component)
                      end-type (cond
                                 (= ::c/endpoint ctype) :end
                                 (= ::c/startpoint ctype) :start)]
                  (api/collision-based-relations-validate app-state entity)
                  (api/collides? app-state
                                 component
                                 f/is-shape-entity
                                 (fn [src trg]
                                   (let [ctype (:type component)
                                         end-type (cond
                                                    (= ::c/endpoint ctype) "end"
                                                    (= ::c/startpoint ctype) "start")]
                                    (e/connect-entities app-state (:entity trg) (:entity src) (keyword end-type))
                                    (std/toggle-controls (:entity trg) false)
                                    (m/refresh-manhattan-layout app-state entity)))
                                 (fn [src]))
                  nil)))

(defbehaviour make-inclusion-relation
              "Include Entity Into Container"
              :make-inclusion-relation
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "mouse-up")
              (fn [e]
                (let [{:keys [app-state component entity] :as event} (:context e)]
                  (api/collision-based-relations-validate app-state entity)
                  (api/collides? app-state
                                 component
                                 f/is-container
                                 (fn [src trg]
                                    (e/connect-entities app-state (:entity trg) (:entity src) :inclusion))
                                 (fn [src]))
                  nil)))

(defbehaviour hovering-entity
              "Default Entity Hovering"
              :hovering
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ::c/entity-shape] "focus")
              (fn [e]
                (let [{:keys [component entity]} (:context e)]
                  (api/component-hover entity component true o/DEFAULT_HIGHLIGHT_OPTIONS)
                  nil)))

(defbehaviour leaving-entity
              "Default Entity Leave"
              :leaving
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ::c/entity-shape] "blur")
              (fn [e]
                (let [{:keys [component entity]} (:context e)]
                  (api/component-hover entity component false o/DEFAULT_HIGHLIGHT_OPTIONS)
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

(defbehaviour show-bbox
              "Show Bounding Box"
              :bbox-show
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/bounding-box] "focus")
              (fn [e]
                (let [event (:context e)]
                  (console.log "bbox")
                  (std/toggle-bbox  (-> event :component) true)
                  nil)))

(defbehaviour hide-bbox
              "Hide Bounding Box"
              :bbox-hide
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/bounding-box] "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-bbox (-> event :component) false)
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
