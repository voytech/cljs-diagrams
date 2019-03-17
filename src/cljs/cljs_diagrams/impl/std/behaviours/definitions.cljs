(ns cljs-diagrams.impl.std.behaviours.definitions
  (:require [cljs-diagrams.core.behaviours :as b]
            [cljs-diagrams.core.eventbus :as bus]
            [cljs-diagrams.core.options :as o]
            [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.events :as ev]
            [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.behaviour-api :as api]
            [cljs-diagrams.core.selection :as s]
            [cljs-diagrams.impl.std.behaviours.behaviour-api :as std]
            [cljs-diagrams.impl.std.behaviours.manhattan :as m]
            [cljs-diagrams.impl.std.shapes :as c]
            [cljs-diagrams.impl.std.features.default :as f])
  (:require-macros [cljs-diagrams.core.macros :refer [defbehaviour]]))

(defbehaviour moving-shape-entity
              "Shapeful Entity Moving"
              :rigid-entity-moving
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "move")
              (fn [e]
                 (let [{:keys [app-state shape node movement-x movement-y]} (:context e)]
                   (api/move-node app-state node movement-x movement-y)
                   nil)))

(defbehaviour moving-association-entity-by
              "Moving Association Entity By"
              :moving-by
              [f/is-association-entity]
              (b/build-event-name "moveby")
              (fn [e]
                (let [event (:context e)
                      node (:node event)
                      app-state (-> event :app-state)]
                  (m/refresh-manhattan-layout app-state node)
                  nil)))

(defbehaviour moving-primary-entity-by
              "Moving Primary Entity Entity By"
              :moving-by
              [f/is-primary-entity]
              (b/build-event-name "moveby")
              (fn [e]
                 (let [{:keys [app-state node movement-x movement-y]} (:context e)]
                   (api/move-node app-state node movement-x movement-y)
                   nil)))

(defbehaviour select-shape-entity
              "Select object"
              :select
              [f/is-primary-entity]
              (b/build-event-name [::c/edit] "activate")
              (fn [e]
                 (let [{:keys [app-state node]} (:context e)]
                    (s/select app-state node)
                    (bus/fire app-state "node.selected" {:selection node})
                    nil)))

(defbehaviour remove-entity
              "Remove object"
              :remove
              [f/is-primary-entity]
              (b/build-event-name [::c/remove] "activate")
              (fn [e]
                 (let [{:keys [app-state node]} (:context e)]
                    (e/remove-node app-state node)
                    nil)))

(defbehaviour moving-association-endpoints
              "Association's endpoints moving [Manhattan]"
              :association-endpoint-moving
              [f/is-association-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ] "move")
              (fn [e]
                (let [event (:context e)
                      {:keys [app-state node shape movement-x movement-y]} event
                      end-type (if (= ::c/startpoint (:type shape)) :start :end)]
                  (m/endpoint-move app-state node end-type movement-x movement-y)
                  (api/collides? app-state
                                 shape
                                 f/has-controls
                                 (fn [src trg] (std/toggle-controls (:node trg) true))
                                 (fn [src]))
                  nil)))

(defbehaviour make-relation
              "Connect Two Entities"
              :make-relation
              [f/is-association-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ] "mouse-up")
              (fn [e]
                (let [{:keys [app-state shape node] :as event} (:context e)
                      ctype (:type shape)
                      end-type (cond
                                 (= ::c/endpoint ctype) :end
                                 (= ::c/startpoint ctype) :start)]
                  (api/collision-based-relations-validate app-state node)
                  (api/collides? app-state
                                 shape
                                 f/is-shape-entity
                                 (fn [src trg]
                                   (let [ctype (:type shape)
                                         end-type (cond
                                                    (= ::c/endpoint ctype) "end"
                                                    (= ::c/startpoint ctype) "start")]
                                    (e/connect-nodes app-state (:node trg) (:node src) (keyword end-type))
                                    (std/toggle-controls (:node trg) false)
                                    (m/refresh-manhattan-layout app-state node)))
                                 (fn [src]))
                  nil)))

(defbehaviour make-inclusion-relation
              "Include Entity Into Container"
              :make-inclusion-relation
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "mouse-up")
              (fn [e]
                (let [{:keys [app-state shape node] :as event} (:context e)]
                  (api/collision-based-relations-validate app-state node)
                  (api/collides? app-state
                                 shape
                                 f/is-container
                                 (fn [src trg]
                                    (e/connect-nodes app-state (:node trg) (:node src) :inclusion))
                                 (fn [src]))
                  nil)))

(defbehaviour hovering-entity
              "Default Entity Hovering"
              :hovering
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ::c/entity-shape] "focus")
              (fn [e]
                (let [{:keys [shape node]} (:context e)]
                  (api/shape-hover node shape true o/DEFAULT_HIGHLIGHT_OPTIONS)
                  nil)))

(defbehaviour leaving-entity
              "Default Entity Leave"
              :leaving
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/startpoint ::c/endpoint ::c/entity-shape] "blur")
              (fn [e]
                (let [{:keys [shape node]} (:context e)]
                  (api/shape-hover node shape false o/DEFAULT_HIGHLIGHT_OPTIONS)
                  nil)))

(defbehaviour show-all-entity-controls
              "Default Show Controls"
              :controls-show
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "focus")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-controls (:node event) true)
                  nil)))

(defbehaviour hide-all-entity-controls
              "Default Hide Controls"
              :controls-hide
              [f/is-primary-entity]
              (b/build-event-name [::c/entity-shape] "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-controls (:node event) false)
                  nil)))

(defbehaviour show-entity-control
              "Default Show Control"
              :control-show
              [f/has-controls]
              (b/build-event-name [::c/control] "focus")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-control  (-> event :shape) true)
                  nil)))

(defbehaviour hide-entity-control
              "Default Hide Control"
              :control-hide
              [f/has-controls]
              (b/build-event-name [::c/control] "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-control (-> event :shape) false)
                  nil)))

(defbehaviour show-bbox
              "Show Bounding Box"
              :bbox-show
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/bounding-box] "focus")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-bbox  (-> event :shape) true)
                  nil)))

(defbehaviour hide-bbox
              "Hide Bounding Box"
              :bbox-hide
              [f/is-association-entity f/is-primary-entity]
              (b/build-event-name [::c/bounding-box] "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-bbox (-> event :shape) false)
                  nil)))

(defbehaviour resize-entity
              "Resize Entity"
              :entity-resize
              [f/has-controls]
              (b/build-event-name [::c/control] "move")
              (fn [e]
                (let [event (:context e)]
                  (std/resize-with-control (:app-state event)
                                           (:node event)
                                           (:shape event)
                                           (:movement-x event)
                                           (:movement-y event))
                  nil)))
