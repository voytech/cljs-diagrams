(ns ui.views.main
  (:require [reagent.core :as reagent :refer [atom]]
            [ui.components.generic.basic :as components]
            [ui.components.tools :as tv]

            [cljs-diagrams.impl.diagrams.commons.tools :as commontools]
            [cljs-diagrams.impl.diagrams.bpmn.tools :as toolbpmn]))

(defn Library [class app-state feedback]
  [:div {:class (:class class)}
    [components/ShapePropertyEditor app-state feedback [:title :notes]]
    [components/Snapshots app-state]
    [:div
      [components/Tabs {:name "Basic Shapes" :view [tv/ToolBox :basic-tools]}
                       {:name "BPMN" :view [tv/ToolBox :bpmn]}
                       {:name "UML" :view [tv/ToolBox :bpmn]}
                       {:name "Other" :view [tv/ToolBox :commons]}]]])
