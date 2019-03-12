(ns cljs-diagrams.impl.diagrams.bpmn.entities
 (:require [cljs-diagrams.core.entities :as e]
           [cljs-diagrams.core.components :as d :refer [layout-attributes]]
           [cljs-diagrams.impl.std.components :as c]
           [cljs-diagrams.core.funcreg :refer [serialize]]
           [cljs-diagrams.impl.diagrams.bpmn.components :as bpmnc]
           [cljs-diagrams.impl.layouts.expression :as w :refer [layout-hints
                                                                position-of
                                                                size-of]]
           [cljs-diagrams.core.layouts :as cl :refer [layout
                                                      weighted-position
                                                      weighted-size
                                                      size
                                                      weighted-origin
                                                      match-parent-size
                                                      match-parent-position
                                                      margins]]
           [cljs-diagrams.impl.std.behaviours.definitions :as bd])
 (:require-macros [cljs-diagrams.core.macros :refer [defentity
                                                     with-components
                                                     with-layouts
                                                     component
                                                     components-templates
                                                     component-template
                                                     resolve-data
                                                     defcomponent-group
                                                     with-tags
                                                     defp]]))


(defentity activity
  {:width 180 :height 150}
  (with-layouts (layout "main" ::w/expression))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :model {:round-x 15 :round-y 15}
                               :layout-attributes (layout-attributes "main"
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/title {
                        :name "title"
                        :model {:text "Activity"}
                        :layout-attributes (layout-attributes "main"
                                                              (layout-hints
                                                                (weighted-position 0.5 0.5)
                                                                (weighted-origin 0.5 0.5)))})
    (component c/entity-controls)))

(defp event-bbox-draw []
  (fn [component]
    (let [width (d/get-width component)]
      {:radius (/ width 2)
       :height (d/get-width component)})))

(defentity process-start
  {:width 40 :height 40}
  (with-layouts (layout "main" ::w/expression))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :rendering-method :draw-circle
                               :model-customizers [(d/bbox-draw event-bbox-draw)]
                               :model {:radius 20}
                               :layout-attributes (layout-attributes "main"
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes "main" 1
                                                                  (layout-hints
                                                                    (position-of "title" 4 0)
                                                                    (size-of "title" 8 0)
                                                                    (weighted-origin 0 0)))})
    (component c/title {
                        :name "title"
                        :model  {:text "Start"}
                        :layout-attributes (layout-attributes "main"
                                                              (layout-hints
                                                                (weighted-position 0.5 1)
                                                                (weighted-origin 0.5 0)))})
    (component c/small-controls)))

(defentity process-end
  {:width 40 :height 40}
  (with-layouts (layout "main" ::w/expression))
  (components-templates
    (component-template "main" {:border-width 5}))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :rendering-method :draw-circle
                               :model-customizers [(d/bbox-draw event-bbox-draw)]
                               :model {:radius 20 :border-width 5}
                               :layout-attributes (layout-attributes "main"
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes "main" 1
                                                                  (layout-hints
                                                                    (position-of "title" 4 0)
                                                                    (size-of "title" 8 0)
                                                                    (weighted-origin 0 0)))})
    (component c/title {
                        :name "title"
                        :model  {:text "End"}
                        :layout-attributes (layout-attributes "main"
                                                              (layout-hints
                                                                (weighted-position 0.5 1)
                                                                (weighted-origin 0.5 0)))})
    (component c/small-controls)))


(defentity gateway
  {:width 40 :height 40}
  (with-layouts (layout "main" ::w/expression))
  (components-templates
    (component-template ::c/entity-shape {:border-width 5}))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :rendering-method :draw-poly-line
                               :model-customizers [(d/bbox-draw bpmnc/diamond-bbox-draw)]
                               :layout-attributes (layout-attributes "main"
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes "main" 1
                                                                   (layout-hints
                                                                     (position-of "title" 4 0)
                                                                     (size-of "title" 8 0)
                                                                     (weighted-origin 0 0)))})
    (component c/title {
                        :name "title"
                        :model {:text "Gateway"}
                        :layout-attributes (layout-attributes "main"
                                                              (layout-hints
                                                                (weighted-position 0.5 1)
                                                                (weighted-origin 0.5 0)))})
    (component c/small-controls)))
