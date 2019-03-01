(ns ui.views.main
  (:require [reagent.core :as reagent :refer [atom]]
            [ui.components.generic.basic :as components]
            [cljs-diagrams.core.utils.dnd :as dnd]
            [ui.components.tools :as tv]

            [cljs-diagrams.impl.tools :as toolsimpl]
            [cljs-diagrams.diagrams.bpmn.tools :as toolbpmn]
            [cljs-diagrams.diagrams.commons.tools :as toolcommons]

            [cljs-diagrams.core.rendering :as rendering]
            [cljs-diagrams.core.selection :as s]
            [cljs-diagrams.core.entities :as e]
            [cljs-diagrams.impl.extensions.resolvers.default :as resolvers]
            [ui.components.workspace :as ws :refer [Workspace]]))

(defn Library [class app-state feedback]
  [:div {:class (:class class)}
    [components/ShapePropertyEditor app-state feedback [:title :notes]]
    [components/Snapshots app-state]
    [:div
      [components/Tabs {:name "Basic Shapes" :view [tv/ToolBox :basic-tools]}
                       {:name "BPMN" :view [tv/ToolBox :bpmn]}
                       {:name "UML" :view [tv/ToolBox :bpmn]}
                       {:name "Other" :view [tv/ToolBox :commons]}]]])
