(ns impl.entities
 (:require [core.entities :as e]
           [core.components :as cc :refer [layout-attributes]]
           [impl.components :as c]
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

(defentity basic-rect
  {:width 180 :height 150}
  (with-layouts (layout ::w/expression w/expression-layout))
  (with-components context
    (component c/entity-shape {:name "body"
                               :model {:round-x 5 :round-y 5}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/title {:name  "title"
                        :model {:text "Object with header"}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 0.1)
                                                                (weighted-origin 0.5 0)))})
    (component c/entity-controls))
  (shape "body"))

(defentity rect-with-icon
  {:width 240 :height 150}
  (with-layouts (layout ::w/expression w/expression-layout))
  (with-components context
    (component c/entity-shape {:name  "body"
                               :model {:round-x 5 :round-y 5}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/title {:name "title"
                        :model {:text "Object with a header and image"}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 0.1)
                                                                (weighted-origin 0.5 0)))})
    (component c/image {:name  "icon-1"
                        :model {:height 40 :width 40 :image-url "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 0.5)
                                                                (weighted-origin 0.5 0.5)))})
    (component c/entity-controls))
  (shape "body"))

(defentity container
  {:width 300 :height 350}
  (with-tags :container)
  (with-layouts (layout ::w/expression w/expression-layout))
  (with-components context
    (component c/entity-shape {:name  "body"
                               :model {:border-style :dotted :z-index :bottom :opacity "0.4"}
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/title {:name "title"
                        :model {:text "Put other shapes here..."}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0 0.1)
                                                                (weighted-origin 0 0)))})
    (component c/entity-controls))
  (shape "body"))

(defentity association
  {:width 180 :height 150}
  (resolve-data {:x1 50 :y1 50 :x2 230 :y2 85})
  (with-layouts (layout ::w/expression w/expression-layout))
  (components-templates
    (component-template ::c/relation {:border-width 3}))
  (with-components context
    (component c/bounding-box {:name "bbox"
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/relation {:name  "connector"
                           :attributes
                            {:decorators
                              {:head ["arrow" "end"]
                               :tail ["start"]}}})
    (component c/startpoint {:name "start" :model {:left 0 :top 0}})

    (component c/endpoint {:name  "end" :model {:visible true
                                                :opacity 0.4
                                                }})
    (component c/arrow {:name "arrow"})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes ::w/expression 1
                                                                  (layout-hints
                                                                    (position-of "title")
                                                                    (size-of "title" 4 4)
                                                                    (weighted-origin 0 0.75)))})
    (component c/title {:name "title"
                        :model {:text "Title."}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 0.5)
                                                                (weighted-origin 0.5 -0.1)))})))

(defentity link
  {:width 180 :height 150}
  (resolve-data {:x1 50 :y1 50 :x2 230 :y2 85})
  (with-layouts (layout ::w/expression w/expression-layout))
  (components-templates
    (component-template ::c/relation {:border-width 2}))
  (with-components context
    (component c/bounding-box {:name "bbox"
                               :layout-attributes (layout-attributes ::w/expression
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/relation {:name  "connector"
                           :model {:stroke-style :dashed}
                           :attributes
                            {:decorators
                              {:head ["end"]
                               :tail ["start"]}}})
    (component c/startpoint {:name "start" :model {:left 0 :top 0}})
    (component c/endpoint {:name  "end" :model {:visible true}})
    (component c/rectangle {:name "bg"
                            :model {:background-color "white"}
                            :layout-attributes (layout-attributes ::w/expression 1
                                                                  (layout-hints
                                                                    (position-of "title")
                                                                    (size-of "title" 4 4)
                                                                    (weighted-origin 0 0.75)))})
    (component c/title {:name "title"
                        :model {:text "Title."}
                        :layout-attributes (layout-attributes ::w/expression
                                                              (layout-hints
                                                                (weighted-position 0.5 0.5)
                                                                (weighted-origin 0.5 -0.1)))})))
