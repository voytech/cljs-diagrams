(ns impl.entities
 (:require [core.entities :as e]
           [core.components :as cc]
           [impl.components :as c]
           [impl.layouts :as l]
           [core.layouts :as cl]
           [impl.behaviours.definitions :as bd])
 (:require-macros [core.macros :refer [defentity with-components with-layouts layout component]]))

(defn title-layout-options [name]
  (fn [entity]
    (let [bbox (cl/get-bbox entity)
          source (first (filter #(= name (:name %)) (e/components-of entity)))]
      {:left (- (/ (:width bbox) 2) (/ (cc/get-width source) 2))
       :top  25})))

(defn icons-layout-options []
  (fn [entity]
    (let [bbox (cl/get-bbox entity)]
      {:left (- (/ (:width bbox) 2) 25)
       :top (- (/ (:height bbox) 2) 25)})))

(defentity rectangle-node
  (with-layouts
    (layout :title l/default-flow-layout (cl/having-layout-property :title) (title-layout-options "title"))
    (layout :icons l/default-flow-layout (cl/having-layout-property :icons) (icons-layout-options)))
  (with-components data options
    (component c/main "body" {:round-x 5 :round-y 5} {})
    (component c/control "connector-left" {} {:side :left})
    (component c/control "connector-right" {} {:side :right})
    (component c/control "connector-top" {} {:side :top})
    (component c/control "connector-bottom" {} {:side :bottom})
    (component c/title "title" {:text "An entity with a title"} {:layout :title})
    (component c/image "icon-1" {:image-url "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"} {:layout :icons})))

(defn- relation-layout-options [entity]
  (let [bbox (cl/get-bbox entity)]
    {:left (/ (:width bbox) 2)
     :top  (/ (:height bbox) 2)}))

(defentity relation
  (with-layouts
    (layout :attributes l/default-flow-layout (cl/having-layout-property :attributes) relation-layout-options))
  (with-components data options
    (component c/relation "connector" {} {:start "start" :end "end"})
    (component c/startpoint "start" {} {})
    (component c/arrow "arrow" {} {})
    (component c/endpoint "end" {} {})
    (component c/title "title" {:text "Title."} {:layout :attributes})))
