(ns ui.components.generic.basic
  (:require [reagent.core :as reagent :refer [atom]]
            [core.project :as p]
            [core.utils.dom :as dom]))

(defn Tabs [& childs]
  (let [active (atom (:name (first childs)))]
    (fn []
      [:div
       [:ul {:class "nav nav-tabs"}
        (doall
          (for [child childs]
           ^{:key (:name child)}
           [:li {:class "nav-item"}
            [:a (if (= (:name child) @active) {:class "nav-link active"} {:class "nav-link" :on-click #(reset! active (:name child))}) (:name child)]]))]
       (:view (first (filter #(= @active (:name %)) childs)))])))

(defn PageThumb [id]
  [:div
    (let [canvas-page (get-in @p/project [:pages (keyword id) :canvas])]
      (when (not (nil? canvas-page))
        [:img {:class "img-thumbnail"
               :id (str "thumb-" id)
               :src (.toDataURL canvas-page)}]))])

(defn DynamicPagination [pages change-page new-page delete-page]
  [:ul {:class "pagination"}
   (for [page pages]
     ^{:key (:id page)}
     [:li {:class "page-item page-thumb"}
       [:a {:on-click #(delete-page (:id page))} [:i {:class "fa fa-times" :aria-hidden "true"}]]
       [:a {:class "page-link page-thumb" :on-click #(change-page (:id page))}
         [PageThumb (name (:id page))]]])
   (when (not (nil? new-page))
     [:li {:class "page-item page-thumb"}
       [:a {:class "page-link page-add" :on-click #(new-page)}
         [:i {:class "fa fa-2x fa-plus-circle" :aria-hidden "true"}]]])])
