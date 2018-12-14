(ns impl.entities
 (:require [core.entities :as e]
           [core.components :as cc]
           [impl.components :as c]
           [impl.layouts :as l]
           [core.layouts :as cl]
           [impl.behaviours.definitions :as bd])
 (:require-macros [core.macros :refer [defentity
                                       with-components
                                       with-layouts
                                       layout
                                       component
                                       components-templates
                                       component-template
                                       named-group]]))

(defn title-layout-options [name]
  (fn [entity]
    (let [bbox (cl/get-bbox entity)
          source (first (filter #(= name (:name %)) (e/components-of entity)))]
      {:left (- (/ (:width bbox) 2) (/ (cc/get-width source) 2))
       :top  25})))

(defn container-title-layout-options [name]
 (fn [entity]
   {:left 10
    :top  25}))

(defn icons-layout-options []
  (fn [entity]
    (let [bbox (cl/get-bbox entity)]
      {:left (- (/ (:width bbox) 2) 25)
       :top (- (/ (:height bbox) 2) 25)})))

(defentity basic-rect
  {:width 180 :height 150}
  (with-layouts
    (layout :title l/default-flow-layout (cl/having-layout-property :title) (title-layout-options "title"))
    (layout :icons l/default-flow-layout (cl/having-layout-property :icons) (icons-layout-options)))
  (with-components data options
    (component c/entity-shape "body" {:round-x 5 :round-y 5} {})
    (component c/title "title" {:text "An entity with a title"} {:layout :title})
    (component c/entity-controls)))

(defentity rect-with-icon
  {:width 180 :height 150}
  (with-layouts
    (layout :title l/default-flow-layout (cl/having-layout-property :title) (title-layout-options "title"))
    (layout :icons l/default-flow-layout (cl/having-layout-property :icons) (icons-layout-options)))
  (with-components data options
    (component c/entity-shape "body" {:round-x 5 :round-y 5} {})
    (component c/title "title" {:text "An entity with a title"} {:layout :title})
    (component c/image "icon-1" {:height 40 :width 40 :image-url "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"} {:layout :icons})
    (component c/entity-controls)))

(defentity container
  {:width 300 :height 350}
  (with-layouts
    (layout :title l/default-flow-layout (cl/having-layout-property :title) (container-title-layout-options "title")))
  (with-components data options
    (component c/entity-shape "body" {:border-style :dotted :z-index :bottom :opacity "0.4"} {})
    (component c/title "title" {:text "Put other shapes here..."} {:layout :title})
    c/entity-controls))

(defn- relation-layout-options [entity]
  (let [bbox (cl/get-bbox entity)]
    {:left (/ (:width bbox) 2)
     :top  (/ (:height bbox) 2)}))

(defentity association
  {:width 180 :height 150}
  (with-layouts
    (layout :attributes l/default-flow-layout (cl/having-layout-property :attributes) relation-layout-options))
  (components-templates
    (component-template ::c/relation {:border-width 3}))
  (with-components data options
    (component c/relation "connector" {} {:start "start" :end "end"})
    (component c/startpoint "start" {} {})
    (component c/arrow "arrow" {} {})
    (component c/endpoint "end" {} {})
    (component c/title "title" {:text "Title."} {:layout :attributes})))
