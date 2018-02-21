(ns ui.views.main
  (:require [reagent.core :as reagent :refer [atom]]
            [ui.components.generic.basic :as components]
            [core.utils.dnd :as dnd]
            [ui.components.tools :as tv]
            [impl.tools :as toolsimpl]
            [impl.renderers.default]
            [ui.components.workspace :as ws :refer [Workspace]]))

;;FUNCTIONS

;;COMPONENTS

(defn Library [class]
  [:div {:class (:class class)}
    [components/Tabs {:name "Toolbox" :view [tv/ToolBox :basic-tools]}
                     {:name "Attributes" :view [:div "Attributes"]}]])


(defn Main []
  [:div.container-fluid
    [:div.row.row-offcanvas.row-offcanvas-left
     [Library {:class "col-2 sidebar-offcanvas"}]
     [Workspace {:class "col"}]]])
