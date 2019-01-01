(ns app.app
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dom :as dom]
            [core.project :as project]
            [impl.behaviours.definitions :as b]
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
       "mouseleave" "mouse-leave"
     }
     :application-events {
       "mouse-dragging-move"   "move"
       "mouse-in"              "focus"
       "mouse-out"             "blur"
       "mouse-point-click"     "activate"
     }
     :patterns {
       :mouse-dragging-move (patterns/mouse-dragging-move-event)
       :mouse-move (patterns/mouse-move-event)
       :mouse-in (patterns/mouse-in-event)
       :mouse-out (patterns/mouse-out-event)
       :mouse-point-click (patterns/mouse-click-event)
     }
   }
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
      b/hide-bbox
   ]})

(defn init []
  (let [config (app-config 1270 1000 :reagentsvg)
        app-state (state/create-app-state "project" config)]
    (reagent/render-component [m/Main app-state config]
      (.getElementById js/document "container"))))
