(ns ui.components.generic.basic
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-diagrams.core.project :as p]
            [cljs-diagrams.core.selection :as s]
            [cljs-diagrams.extensions.data-resolvers :as resolvers]
            [cljs-diagrams.core.utils.dom :as dom]))

(defn Tabs [& childs]
  (let [active (atom (:name (first childs)))]
    (fn []
      [:div
       [:ul {:class "nav nav-tabs"}
        (doall
          (for [child childs]
           ^{:key (:name child)}
           [:li {:class "nav-item"}
            [:a (if (= (:name child) @active) {:class "nav-link active"} {:class "nav-link" :on-click #(reset! active (:name child))})
                (if (= (:name child) @active) [:b (:name child)] (:name child))]]))]
       (:view (first (filter #(= @active (:name %)) childs)))])))

(defn ShapePropertyEditor [app-state feedback properties]
  [:div {:id "shape-property-editor"
         :class "modal"
         :style {:display (if (:show-popup @feedback) "block" "none")}}
    [:div {:class "modal-content"}
      (let [state (atom {})]
        [:form
          (for [property properties]
            ^{:key property}
            [:input {:id (str "input-" (name property))
                     :placeholder (str "Enter " (name property))
                     :on-change #(swap! state assoc-in [property] (-> % .-target .-value))}])
          [:button {:type "button"
                    :on-click (fn []
                                  (swap! feedback assoc :show-popup false)
                                  (resolvers/apply-data
                                      app-state
                                      (:selection @feedback)
                                      @state))}
                   "Submit"]
          [:button {:type "button" :on-click #(swap! feedback assoc :show-popup false)} "Cancel"]])]])
