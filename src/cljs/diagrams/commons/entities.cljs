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
  (resolve-data {:notes "Here we can wrtie some multiline notes. Not too long tough."})
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
    (component c/entity-controls)))
