(ns views.main
  (:require [reagent.core :as reagent :refer [atom]]
            [components.basic :as components]))


;;FUNCTIONS
(defn resolve-drop "Should resolve drop events for following scenarios:
                    1. The drag source is from the library component (image is dragged)
                    2. The drag source comes from desktop (Use FileApi to load image)"
   [event]
   (.preventDefault event)
   (dnd/dispatch-drop-event event))
;;COMPONENTS

(defn library [class]
  [:div {:class (:class class)}
    [components/tabs {:name "Photos" :view [:div "Photos"]}
                     {:name "Toolbox" :view [:div "Toolbox"]}
                     {:name "Templates" :view [:div "Templates"]}]])

(defn workspace[class]
  [:div {:id "workspace-iner" :class (:class class)}
    [:div {:id "canvas-wrapper"
           :class "workspace-div"
           :drop resolve-drop
           :dragover #(.preventDefault %)}]
    [:div {:id "zoom-control"
           :class "zoom-control-div"}
      [:div {:class "pull-right"} "History controls"]
      [:div {:class "pull-right col-md-4"} "Zoom controls"]]])

(defn main-view []
  [:div.container-fluid
    [:div.row
     [library {:class "col-3"}]
     [workspace {:class "col"}]]])
