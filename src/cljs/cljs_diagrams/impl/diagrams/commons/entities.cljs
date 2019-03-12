(ns cljs-diagrams.impl.diagrams.commons.entities
 (:require [cljs-diagrams.core.entities :as e]
           [cljs-diagrams.core.components :as d :refer [layout-attributes]]
           [cljs-diagrams.impl.std.components :as c]
           [cljs-diagrams.impl.diagrams.bpmn.components :as bpmnc]
           [cljs-diagrams.impl.layouts.flow :as f]
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
                                                     with-tags]]))

(defentity basic-rect
  {:width 180 :height 150}
  (with-layouts (layout "main" ::w/expression))
  (with-components context
                   (component c/entity-shape {:name "body"
                                              :model {:round-x 5 :round-y 5}
                                              :layout-attributes (layout-attributes "main"
                                                                                    (layout-hints
                                                                                     (match-parent-position)
                                                                                     (match-parent-size)
                                                                                     (weighted-origin 0 0)))})
                   (component c/title {:name  "title"
                                       :model {:text "Object with header"}
                                       :layout-attributes (layout-attributes "main"
                                                                             (layout-hints
                                                                              (weighted-position 0.5 0.1)
                                                                              (weighted-origin 0.5 0)))})
                   (component c/entity-controls)
                   (component c/shape-editing)))


(defentity rect-with-icon
  {:width 240 :height 150}
  (with-layouts (layout "main" ::w/expression))
  (with-components context
                   (component c/entity-shape {:name  "body"
                                              :model {:round-x 5 :round-y 5}
                                              :layout-attributes (layout-attributes "main"
                                                                                    (layout-hints
                                                                                     (match-parent-position)
                                                                                     (match-parent-size)
                                                                                     (weighted-origin 0 0)))})
                   (component c/title {:name "title"
                                       :model {:text "Object with a header and image"}
                                       :layout-attributes (layout-attributes "main"
                                                                             (layout-hints
                                                                              (weighted-position 0.5 0.1)
                                                                              (weighted-origin 0.5 0)))})
                   (component c/image {:name  "icon-1"
                                       :model {:height 40 :width 40 :image-url "/icons/settings.svg"}
                                       :layout-attributes (layout-attributes "main"
                                                                             (layout-hints
                                                                              (weighted-position 0.5 0.5)
                                                                              (weighted-origin 0.5 0.5)))})
                   (component c/entity-controls)))

(defentity container
  {:width 300 :height 350}
  (with-tags :container)
  (with-layouts (layout "main" ::w/expression))
  (with-components context
                   (component c/entity-shape {:name  "body"
                                              :model {:border-style :dotted :z-index :bottom :opacity "0.4"}
                                              :layout-attributes (layout-attributes "main"
                                                                                    (layout-hints
                                                                                     (match-parent-position)
                                                                                     (match-parent-size)
                                                                                     (weighted-origin 0 0)))})
                   (component c/title {:name "title"
                                       :model {:text "Put other shapes here..."}
                                       :layout-attributes (layout-attributes "main"
                                                                             (layout-hints
                                                                              (weighted-position 0 0.1)
                                                                              (weighted-origin 0 0)))})
                   (component c/entity-controls)))

(defentity association
  {:width 180 :height 150}
  (resolve-data {:x1 0 :y1 0 :x2 230 :y2 85})
  (with-layouts (layout "main" ::w/expression))
  (components-templates
   (component-template "connector" {:border-width 3}))
  (with-components context
                   (component c/bounding-box {:name "bbox"
                                              :layout-attributes (layout-attributes "main"
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
                   (component c/endpoint {:name "end" :model {:visible false}})
                   (component c/arrow {:name "arrow"})
                   (component c/rectangle {:name "bg"
                                           :model {:background-color "white"}
                                           :layout-attributes (layout-attributes "main" 1
                                                                                 (layout-hints
                                                                                  (position-of "title" 4 2)
                                                                                  (size-of "title" 8 4)
                                                                                  (weighted-origin 0 0)))})
                   (component c/title {:name "title"
                                       :model {:text "Title."}
                                       :layout-attributes (layout-attributes "main"
                                                                             (layout-hints
                                                                              (weighted-position 0.5 0.5)
                                                                              (weighted-origin 0.5 0)))})))

(defentity link
  {:width 180 :height 150}
  (resolve-data {:x1 0 :y1 0 :x2 230 :y2 85})
  (with-layouts (layout "main" ::w/expression))
  (components-templates
   (component-template "connector" {:border-width 2}))
  (with-components context
                   (component c/bounding-box {:name "bbox"
                                              :layout-attributes (layout-attributes "main"
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
                   (component c/endpoint {:name  "end" :model {:visible true}})))

(defentity note
  {:width 180 :height 150}
  (resolve-data {:notes "Here we can wrtie some multiline notes. Not too long tough."})
  (with-layouts (layout "main" ::w/expression)
                (layout "notes" ::f/flow (weighted-position 0.1 0.1) (weighted-size 0.8 0.8) nil))
  (with-components context
    (component c/entity-shape {
                               :name "main"
                               :model {:stroke-style :dashed :background-color "#ffff99"}
                               :layout-attributes (layout-attributes "main"
                                                                     (layout-hints
                                                                       (match-parent-position)
                                                                       (match-parent-size)
                                                                       (weighted-origin 0 0)))})
    (component c/entity-controls)))
