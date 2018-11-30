(ns impl.entities
 (:require [core.entities :as e]
           [core.components :as cc]
           [impl.components :as c]
           [impl.layouts :as l]
           [core.layouts :as cl]
           [impl.behaviours.definitions :as bd])
 (:require-macros [core.macros :refer [defentity with-components with-layouts layout]]))

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
      [#(c/main %1 %2 "body"  {:round-x 5 :round-y 5} {})
       #(c/control %1 %2 "connector-left" {} {:side :left})
       #(c/control %1 %2 "connector-right" {} {:side :right})
       #(c/control %1 %2 "connector-top" {} {:side :top})
       #(c/control %1 %2 "connector-bottom" {} {:side :bottom})
       #(c/title %1 %2 "title" {:text "An entity with a title"} {:layout :title})
       #(c/image %1 %2 "icon-1" {:image-url "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"} {:layout :icons})]))

(defn- relation-layout-options [entity]
  (let [bbox (cl/get-bbox entity)]
    {:left (/ (:width bbox) 2)
     :top  (/ (:height bbox) 2)}))

(defentity relation
  (with-layouts
    (layout :attributes l/default-flow-layout (cl/having-layout-property :attributes) relation-layout-options))
  (with-components data options
     [#(c/relation %1 %2 "connector" {} {:start "start" :end "end"})
      #(c/startpoint %1 %2"start" {} {})
      #(c/arrow %1 %2 "arrow" {} {})
      #(c/endpoint %1 %2 "end" {} {})
      #(c/title %1 %2 "title" {:text "Title."} {:layout :attributes})]))
