(ns diagrams.commons.entities
 (:require [core.entities :as e]
           [core.components :as d :refer [layout-attributes]]
           [impl.components :as c]
           [diagrams.bpmn.components :as bpmnc]
           [impl.layouts.expression :as w :refer [layout-hints
                                                  position-of
                                                  size-of]]
           [core.layouts :as cl :refer [layout
                                        weighted-position
                                        weighted-size
                                        size
                                        weighted-origin
                                        match-parent-size
                                        match-parent-position
                                        margins]]
           [impl.behaviours.definitions :as bd])
 (:require-macros [core.macros :refer [defentity
                                       with-components
                                       with-layouts
                                       component
                                       components-templates
                                       component-template
                                       resolve-data
                                       defcomponent-group
                                       with-tags]]))


(defentity note
  {:width 180 :height 150}
  (with-layouts (layout ::w/expression w/expression-layout))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :model {:stroke-style :dashed :background-color "#ffff99"}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/textarea {}
                        :name "note"
                        :model {:text "Lorem ipsum dolor sit amet,
                                       consectetur adipisicing elit,
                                       sed do eiusmod tempor incididunt
                                       ut labore et dolore magna aliqua.
                                       Ut enim ad minim veniam, quis
                                       nostrud exercitation ullamco laboris
                                       nisi ut aliquip ex ea commodo consequat.
                                       Duis aute irure dolor in reprehenderit
                                       in voluptate velit esse cillum dolore
                                       eu fugiat nulla pariatur.
                                       Excepteur sint occaecat cupidatat non proident,
                                       sunt in culpa qui officia deserunt mollit
                                       anim id est laborum"
                                :word-wrap :break-words
                                :text-overflow :hidden}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (match-parent-position)
                                                                (match-parent-size)
                                                                (weighted-origin 0 0))))
    (component c/entity-controls)))
