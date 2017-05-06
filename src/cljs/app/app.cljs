(ns app.app
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dom :as dom]
            [core.project :as project]
            [ui.views.main :as m]))

(defn init []
  (reagent/render-component [m/Main]
    (.getElementById js/document "container")))
