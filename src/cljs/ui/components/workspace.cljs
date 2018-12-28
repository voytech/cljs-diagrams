(ns ui.components.workspace
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dnd :as dnd]
            [core.state :as state]
            [ui.components.generic.basic :as cmp :refer [ShapePropertyEditor]]
            [core.project :as p]
            [core.eventbus :as b]
            [impl.synthetic-events :as s]
            [core.tools :as t :refer [tools]]
            [impl.synthetic-events :as patterns]))


(defn resolve-drop "Should resolve drop events for following scenarios:
                    1. The drag source is from the library component (image is dragged)
                    2. The drag source comes from desktop (Use FileApi to load image)"
  [state]
  (fn [event]
    (let [e (.-nativeEvent event)]
      (.preventDefault e)
      (dnd/dispatch-drop-event e state))))


(defn canvas-initializing-wrapper [app-state feedback config]
 (with-meta identity
   {:component-did-mount (fn [el]
                            (let [domid (.-id (reagent/dom-node el))]
                              (p/initialize domid app-state config)
                              (b/on app-state ["entity.selected"] -999
                                (fn [event]
                                  (swap! feedback assoc :selection (-> event :context :selection)
                                                        :show-popup true)
                                  nil))))}))

(defn Workspace [class app-state config]
  [:div {:id "workspace-inner" :class (:class class)}
    (let [feedback (atom {})]
      [:div
        [ShapePropertyEditor app-state feedback [:title]]
        [:div {:id "canvas-wrapper"
               :class "workspace-div"
               :on-drop (resolve-drop app-state)
               :on-drag-over #(.preventDefault %)}
           [(canvas-initializing-wrapper app-state feedback config)
             [:div {:id "project" :class "canvas"}]]]])])
