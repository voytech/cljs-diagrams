(ns ui.views.main
  (:require [reagent.core :as reagent :refer [atom]]
            [ui.components.generic.basic :as components]
            [core.utils.dnd :as dnd]
            [ui.components.tools :as tv]
            [impl.tools :as toolsimpl]
            [diagrams.bpmn.tools :as toolbpmn]
            [core.rendering :as rendering]
            [impl.renderers.reagentsvg]
            [core.selection :as s]
            [core.entities :as e]
            [impl.extensions.resolvers.default :as resolvers]
            [ui.components.workspace :as ws :refer [Workspace]]))

(defn Library [class app-state feedback]
  [:div {:class (:class class)}
    [components/ShapePropertyEditor app-state feedback [:title]]
    [:div
      [components/Tabs {:name "Basic Shapes" :view [tv/ToolBox :basic-tools]}
                       {:name "BPMN" :view [tv/ToolBox :bpmn]}
                       {:name "UML" :view [tv/ToolBox :bpmn]}
                       {:name "Other" :view [tv/ToolBox :bpmn]}]]])
