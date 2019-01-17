(ns diagrams.bpmn.entities
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
                                       shape
                                       with-tags]]))


(defentity activity
  {:width 180 :height 150}
  (with-layouts (layout ::w/expression w/expression-layout))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :model {:round-x 15 :round-y 15}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/title {
                        :name "title"
                        :model {:text "Activity"}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 0.5)
                                                                (weighted-origin 0.5 0.5)))})
    (component c/entity-controls))
  (shape "main"))

(defn event-bbox-draw []
  (fn [component]
    (let [width (d/get-width component)]
      {:radius (/ width 2)
       :height (d/get-width component)})))

(defentity process-start
  {:width 40 :height 40}
  (with-layouts (layout ::w/expression w/expression-layout))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :rendering-method :draw-circle
                               :bbox-draw (event-bbox-draw)
                               :model {:radius 20}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes ::w/expression 1
                                                                  (layout-hints
                                                                    (position-of "title")
                                                                    (size-of "title" 4 4)
                                                                    (weighted-origin 0 0.75)))})
    (component c/title {
                        :name "title"
                        :model  {:text "Start"}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 1)
                                                                (weighted-origin 0.5 -1.5)))})
    (component c/small-controls))
  (shape "main"))

(defentity process-end
  {:width 40 :height 40}
  (with-layouts (layout ::w/expression w/expression-layout))
  (components-templates
    (component-template "main" {:border-width 5}))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :rendering-method :draw-circle
                               :bbox-draw (event-bbox-draw)
                               :model {:radius 20 :border-width 5}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes ::w/expression 1
                                                                  (layout-hints
                                                                    (position-of "title")
                                                                    (size-of "title" 4 4)
                                                                    (weighted-origin 0 0.75)))})
    (component c/title {
                        :name "title"
                        :model  {:text "End"}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 1)
                                                                (weighted-origin 0.5 -1.5)))})
    (component c/small-controls))
  (shape "main"))


(defentity gateway
  {:width 40 :height 40}
  (with-layouts (layout ::w/expression w/expression-layout))
  (components-templates
    (component-template ::c/entity-shape {:border-width 5}))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :rendering-method :draw-poly-line
                               :bbox-draw (bpmnc/diamond-bbox-draw)
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes ::w/expression 1
                                                                   (layout-hints
                                                                     (position-of "title")
                                                                     (size-of "title" 4 4)
                                                                     (weighted-origin 0 0.75)))})
    (component c/title {
                        :name "title"
                        :model {:text "Gateway"}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 1)
                                                                (weighted-origin 0.5 -1.5)))})
    (component c/small-controls))
  (shape "main"))
