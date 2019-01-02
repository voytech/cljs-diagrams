(ns app.app
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dom :as dom]
            [core.utils.dnd :as dnd]
            [core.project :as project]
            [impl.behaviours.definitions :as b]
            [impl.renderers.svg :as svg]
            [core.state :as state]
            [impl.synthetic-events :as patterns]
            [ui.views.main :as m]))

(defn app-config [width height renderer]
  {:width width
   :height height
   :renderer renderer
   :events {
            :canonical-events {
                               "mousedown"  "mouse-down"
                               "mouseup"    "mouse-up"
                               "click"      "mouse-click"
                               "dbclick"    "mouse-db-click"
                               "mousemove"  "mouse-move"
                               "mouseenter" "mouse-enter"
                               "mouseleave" "mouse-leave"}

            :application-events {
                                 "mouse-dragging-move"   "move"
                                 "mouse-in"              "focus"
                                 "mouse-out"             "blur"
                                 "mouse-point-click"     "activate"}

            :patterns {
                       :mouse-dragging-move (patterns/mouse-dragging-move-event)
                       :mouse-move (patterns/mouse-move-event)
                       :mouse-in (patterns/mouse-in-event)
                       :mouse-out (patterns/mouse-out-event)
                       :mouse-point-click (patterns/mouse-click-event)}}


   :behaviours [
                b/moving-rigid-entity
                b/moving-association-entity-by
                b/moving-primary-entity-by
                b/moving-association-endpoints
                b/make-relation
                b/make-inclusion-relation
                b/hovering-entity
                b/leaving-entity
                b/show-all-entity-controls
                b/hide-all-entity-controls
                b/show-entity-control
                b/hide-entity-control
                b/resize-entity
                b/select-shape-entity
                b/show-bbox
                b/hide-bbox]})


(defn resolve-drop "Should resolve drop events for following scenarios:
                   1. The drag source is from the library component (image is dragged)
                   2. The drag source comes from desktop (Use FileApi to load image)"
  [state]
  (fn [event]
    (.preventDefault event)
    (dnd/dispatch-drop-event event state)))

(defn init []
  (let [config (app-config 1270 1000 :svg)
        app-state (state/create-app-state "project" config)]
    (reagent/render-component [m/Library {:class "col-8 sidebar-offcanvas"} app-state]
      (.getElementById js/document "reagent-panel-app"))
    (let [canvas-wrapper (dom/by-id "canvas-wrapper")]
      (.addEventListener canvas-wrapper "drop" (resolve-drop app-state))
      (.addEventListener canvas-wrapper "dragover" #(.preventDefault %))
      (project/initialize "project" app-state config))))
