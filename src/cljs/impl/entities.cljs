(ns impl.entities
 (:require [core.entities :as e]
           [core.components :as cc]
           [impl.components :as c]
           [impl.layouts.weighted :as w :refer [layout-hints]]
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
                                       defcomponent-group
                                       shape
                                       with-tags]]))

(defentity basic-rect
  {:width 180 :height 150}
  (with-layouts (layout ::w/weighted w/weighted-layout))
  (with-components context
    (component c/entity-shape "body" {:round-x 5 :round-y 5} {}
      (layout-hints (match-parent-position) (match-parent-size) (weighted-origin 0 0)) ::w/weighted)
    (component c/title "title" {:text "Object with header"} {}
      (layout-hints (weighted-position 0.5 0.1) (weighted-origin 0.5 0)) ::w/weighted)
    (component c/entity-controls))
  (shape "body"))

(defentity rect-with-icon
  {:width 240 :height 150}
  (with-layouts (layout ::w/weighted w/weighted-layout))
  (with-components context
    (component c/entity-shape "body" {:round-x 5 :round-y 5} {}
      (layout-hints (match-parent-position) (match-parent-size) (weighted-origin 0 0)) ::w/weighted)
    (component c/title "title" {:text "Object with a header and image"} {}
      (layout-hints (weighted-position 0.5 0.1) (weighted-origin 0.5 0)) ::w/weighted)
    (component c/image "icon-1" {:height 40 :width 40 :image-url "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"} {}
      (layout-hints (weighted-position 0.5 0.5) (weighted-origin 0.5 0.5)) ::w/weighted)
    (component c/entity-controls))
  (shape "body"))

(defentity container
  {:width 300 :height 350}
  (with-tags :container)
  (with-layouts (layout ::w/weighted w/weighted-layout))
  (with-components context
    (component c/entity-shape "body" {:border-style :dotted :z-index :bottom :opacity "0.4"} {}
      (layout-hints (match-parent-position) (match-parent-size) (weighted-origin 0 0)) ::w/weighted)
    (component c/title "title" {:text "Put other shapes here..."} {}
      (layout-hints (weighted-position 0 0.1) (weighted-origin 0 0)) ::w/weighted)
    (component c/entity-controls))
  (shape "body"))

(defentity association
  {:width 180 :height 150}
  (with-layouts (layout ::w/weighted w/weighted-layout))
  (components-templates
    (component-template ::c/relation {:border-width 3}))
  (with-components context
    (component c/bounding-box "bbox" {} {}
      (layout-hints (match-parent-position) (match-parent-size) (weighted-origin 0 0)) ::w/weighted)
    (component c/relation "connector" {} {:start "start" :end "end"})
    (component c/startpoint "start" {:left 0 :top 0} {})
    (component c/arrow "arrow" {} {})
    (component c/endpoint "end" {} {})
    (component c/rectangle "bg" {:background-color "white" :width 100 :height 18} {}
      (layout-hints (weighted-position 0.5 0.5) (weighted-origin 0.5 0.5)) ::w/weighted)
    (component c/title "title" {:text "Title."} {}
      (layout-hints (weighted-position 0.5 0.5) (weighted-origin 0.5 -0.1)) ::w/weighted)))
