(ns impl.entities
 (:require [core.entities :as e]
           [core.components :as cc]
           [impl.components :as c]
           [impl.layouts :as l]
           [core.layouts :as cl :refer [layout
                                        relative-position
                                        relative-size
                                        relative-origin
                                        fill-size
                                        no-offset
                                        margins
                                        layout-hints]]
           [impl.behaviours.definitions :as bd])
 (:require-macros [core.macros :refer [defentity
                                       with-components
                                       with-layout
                                       component
                                       components-templates
                                       component-template
                                       named-group
                                       shape
                                       with-tags]]))

(defentity basic-rect
  {:width 180 :height 150}
  (with-layout
    (layout :main l/relative-layout))
  (with-components context
    (component c/entity-shape "body" {:round-x 5 :round-y 5} {}
      (layout-hints (no-offset) (fill-size)))
    (component c/title "title" {:text "Object with header"} {}
      (layout-hints (relative-position 0.5 0.05) (relative-origin 0.5 0)))
    (component c/entity-controls))
  (shape "body"))

(defentity rect-with-icon
  {:width 180 :height 150}
  (with-layout
    (layout :main l/relative-layout))
  (with-components context
    (component c/entity-shape "body" {:round-x 5 :round-y 5} {} nil)
    (component c/title "title" {:text "Object with a header and image"} {:layout :title} nil)
    (component c/image "icon-1" {:height 40 :width 40 :image-url "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"} {:layout :icons}
      (layout-hints (relative-position 0.5 0.5) (relative-origin 0.5 0.5)))
    (component c/entity-controls))
  (shape "body"))

(defentity container
  {:width 300 :height 350}
  (with-tags :container)
  (with-layout
    (layout :main l/relative-layout))
  (with-components context
    (component c/entity-shape "body" {:border-style :dotted :z-index :bottom :opacity "0.4"} {} nil)
    (component c/title "title" {:text "Put other shapes here..."} {} nil)
    (component c/entity-controls))
  (shape "body"))

(defentity association
  {:width 180 :height 150}
  (with-layout
    (layout :main l/relative-layout))
  (components-templates
    (component-template ::c/relation {:border-width 3}))
  (with-components context
    (component c/relation "connector" {} {:start "start" :end "end"} nil)
    (component c/startpoint "start" {} {} nil)
    (component c/arrow "arrow" {} {} nil)
    (component c/endpoint "end" {} {} nil)
    (component c/title "title" {:text "Title."} {} nil)))
