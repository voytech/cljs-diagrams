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
                               :model {:stroke-style :dashed}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/text {
                        :name "note"
                        :model {:text "Write some notes..."}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 0.5)
                                                                (weighted-origin 0.5 0.5)))})
    (component c/entity-controls)))
