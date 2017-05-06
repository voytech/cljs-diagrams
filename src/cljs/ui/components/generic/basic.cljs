(ns ui.components.generic.basic
  (:require [reagent.core :as reagent :refer [atom]]
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
 (let [canvas-page (dom/by-id id)]
   (fn []
     (when (not (nil? canvas-page))
       [:img {:class "page-thumb"
              :id (str "thumb-" id)
              :src (.toDataURL canvas-page)}]))))

(defn DynamicPagination [pages change-page new-page]
  [:ul {:class "pagination"}
   (for [page pages]
     ^{:key (:id page)}
     [:li {:class "page-item page-thumb"}
       [:a {:class "page-link page-thumb" :on-click #(change-page (:id page))}
         [PageThumb (name (:id page))]]])
   (when (not (nil? new-page))
     [:li {:class "page-item page-thumb"}
       [:a {:class "page-link page-thumb" :on-click #(new-page)} " + "]])])
