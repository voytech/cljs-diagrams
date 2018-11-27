(ns impl.entities
 (:require [core.entities :as e]
           [core.components :as cc]
           [core.options :as defaults]
           [impl.standard-attributes :as stdatr]
           [impl.components :as c]
           [impl.layouts :as l]
           [core.layouts :as cl]
           [core.project :as p]
           [core.eventbus :as bus]
           [core.options :as o]
           [impl.behaviours.definitions :as bd]
           [clojure.string :as str])
 (:require-macros [core.macros :refer [defentity with-components with-layouts layout]]))

(defn title-layout-options [name]
  (fn [e]
    (let [bbox (cl/get-bbox e)
          source (e/get-entity-component e name)]
      (console.log (clj->js source))    
      {:left (- (/ (:width bbox) 2) (/ (cc/get-width source) 2))
       :top  25})))

(defn icons-layout-options []
  (fn [e]
    (let [bbox (cl/get-bbox e)]
      {:left (- (/ (:width bbox) 2) 25)
       :top (- (/ (:height bbox) 2) 25)})))

(defentity rectangle-node
  (with-layouts
    (layout :title l/default-flow-layout (cl/having-layout-property :title) (title-layout-options "title"))
    (layout :icons l/default-flow-layout (cl/having-layout-property :icons) (icons-layout-options)))
  (with-components data options
      [#(c/main % "body"  {:round-x 5 :round-y 5} {})
       #(c/control % "connector-left" {} {:side :left})
       #(c/control % "connector-right" {} {:side :right})
       #(c/control % "connector-top" {} {:side :top})
       #(c/control % "connector-bottom" {} {:side :bottom})
       #(c/title % "title" {:text "An entity with a title"} {:layout :title})
       #(c/image % "icon-1" {:image-url "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"} {:layout :icons})]))

(defn- relation-layout-options [e]
  (let [bbox (cl/get-bbox e)]
    {:left (/ (:width bbox) 2)
     :top  (/ (:height bbox) 2)}))

(defentity relation
  (with-layouts
    (layout :attributes l/default-flow-layout (cl/having-layout-property :attributes) relation-layout-options))
  (with-components data options
     [#(c/relation % "connector" {} {:start "start" :end "end"})
      #(c/startpoint % "start" {} {})
      #(c/arrow % "arrow" {} {})
      #(c/endpoint % "end" {} {})
      #(c/title % "title" {:text "Title."} {:layout :attributes})]))
