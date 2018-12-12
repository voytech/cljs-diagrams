(ns ui.views.main
  (:require [reagent.core :as reagent :refer [atom]]
            [ui.components.generic.basic :as components]
            [core.utils.dnd :as dnd]
            [ui.components.tools :as tv]
            [impl.tools :as toolsimpl]
            [core.rendering :as rendering]
            [impl.renderers.reagentsvg]
            [ui.components.workspace :as ws :refer [Workspace]]))


(defn Library [class]
  [:div {:class (:class class)}
    [components/Tabs {:name "Tools" :view [tv/ToolBox :basic-tools]}
                     {:name "Attributes" :view [:div "Attributes"]}]])


(defn Main []
  [:div.container-fluid
    [:div.row.row-offcanvas.row-offcanvas-left
     [Library {:class "col-8 sidebar-offcanvas"}]
     [Workspace {:class "col"}]]])
