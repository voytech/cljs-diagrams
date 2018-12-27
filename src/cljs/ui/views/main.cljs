(ns ui.views.main
  (:require [reagent.core :as reagent :refer [atom]]
            [ui.components.generic.basic :as components]
            [core.utils.dnd :as dnd]
            [ui.components.tools :as tv]
            [impl.tools :as toolsimpl]
            [core.rendering :as rendering]
            [impl.renderers.reagentsvg]
            [core.selection :as s]
            [core.entities :as e]
            [impl.extensions.resolvers.default :as resolvers]
            [ui.components.workspace :as ws :refer [Workspace]]))

(defn Library [class app-state]
  [:div {:class (:class class)}
    [components/Tabs {:name "Tools" :view [tv/ToolBox :basic-tools]}
                     {:name "Editing" :view
                        [:div {:on-click (fn []
                                           (resolvers/set-title app-state
                                                                (s/get-selected-entity app-state)
                                                                {:title "Hahah!"}))
                              }  "Set title" ]}]])

(defn Main [app-state config]
  [:div.container-fluid
    [:div.row.row-offcanvas.row-offcanvas-left
     [Library {:class "col-8 sidebar-offcanvas"} app-state]
     [Workspace {:class "col"} app-state config]]])
