(ns app.app
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-diagrams.core.utils.dom :as dom]
            [cljs-diagrams.core.utils.dnd :as dnd]
            [cljs-diagrams.core.project :as project]
            [cljs-diagrams.impl.behaviours.definitions :as b]
            [cljs-diagrams.core.eventbus :as bus]
            [cljs-diagrams.impl.renderers.svg :as svg]
            [cljs-diagrams.core.state :as state]
            [cljs-diagrams.impl.synthetic-events :as patterns]
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
                b/moving-shape-entity
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
                b/remove-entity
                b/show-bbox
                b/hide-bbox]})


(defn resolve-drop "Should resolve drop events for following scenarios:
                   1. The drag source is from the library component (image is dragged)
                   2. The drag source comes from desktop (Use FileApi to load image)"
  [state]
  (fn [event]
    (.preventDefault event)
    (dnd/dispatch-drop-event event state)))

(defn register-selection-feedback [app-state feedback]
  (bus/on app-state ["entity.selected"] -999
    (fn [event]
      (swap! feedback assoc :selection (-> event :context :selection)
                            :show-popup true)
      nil)))

(defn init []
  (let [config (app-config 1270 1000 :svg)
        feedback (atom {})
        app-state (state/create-app-state "project" config)]
    (reagent/render-component [m/Library {:class "toolbox"} app-state feedback]
      (.getElementById js/document "reagent-panel-app"))
    (let [canvas-wrapper (dom/by-id "canvas-wrapper")]
      (.addEventListener canvas-wrapper "drop" (resolve-drop app-state))
      (.addEventListener canvas-wrapper "dragover" #(.preventDefault %))
      (project/initialize "project" app-state config)
      (register-selection-feedback app-state feedback))))
