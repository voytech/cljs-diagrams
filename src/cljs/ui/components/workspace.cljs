(ns ui.components.workspace
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dnd :as dnd]
            [ui.components.generic.basic :as cmp :refer [DynamicPagination]]
            [core.project :as p :refer [project]]
            [impl.synthetic-events :as s]
            [core.tools :as t :refer [tools]]))


(defn resolve-drop "Should resolve drop events for following scenarios:
                    1. The drag source is from the library component (image is dragged)
                    2. The drag source comes from desktop (Use FileApi to load image)"
   [event]
   (let [e (.-nativeEvent event)]
     (.preventDefault e)
     (dnd/dispatch-drop-event e)))

(def canvas-initializing-wrapper
 (with-meta identity
   {:component-did-mount #(p/initialize (.-id (reagent/dom-node %)) {:width 1270 :height 1000 :renderer :svg})}))

(defn ProjectCanvas [id]
  (fn []
    [:div
      [canvas-initializing-wrapper
        [:div {:id id :class "canvas"}]]]))

(defn Workspace [class]
  [:div {:id "workspace-inner" :class (:class class)}
    [:div {:id "canvas-wrapper"
           :class "workspace-div"
           :on-drop resolve-drop
           :on-drag-over #(.preventDefault %)}
       [ProjectCanvas "project-page"]]])
    ;[:div {:id "pagination" :class "center"} [DynamicPagination (vals (:pages @project)) p/select-page p/add-page p/remove-page]]
