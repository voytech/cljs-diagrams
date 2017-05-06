(ns components.basic
  (:require [reagent.core :as reagent :refer [atom]]))


(defn tabs [& childs]
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
