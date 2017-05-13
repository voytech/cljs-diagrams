(ns ui.components.workspace
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dnd :as dnd]
            [ui.components.generic.basic :as cmp :refer [DynamicPagination]]
            [core.project :as p :refer [project]]
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
   {:component-did-mount #(p/initialize-page (.-id (reagent/dom-node %)) {:width 500 :height 500})}))

(defn FabricCanvas [id]
  (fn []
    [:div {:style {:display (if (= (keyword id) (:current-page-id @project)) "block" "none")}}
      [canvas-initializing-wrapper
        [:canvas {:id id :class "canvas"}]]]))


(defn Workspace [class]
  [:div {:id "workspace-inner" :class (:class class)}
    [:div {:id "canvas-wrapper"
           :class "workspace-div"
           :on-drop resolve-drop
           :on-drag-over #(.preventDefault %)}
      (doall
        (for [page (vals (:pages @project)) idx (range (count (keys (:pages @project))))]
          ^{:key idx}
          [FabricCanvas (str "page-" idx)]))]
    [:div {:id "pagination" :class "center"} [DynamicPagination (vals (:pages @project)) p/select-page p/add-page p/remove-page]]
    [:div {:id "zoom-control"
           :class "zoom-control-div"}
      [:div {:class "pull-right"} "History controls"]
      [:div {:class "pull-right col-md-4"} "Zoom controls"]]])
