(ns impl.entities
 (:require [core.entities :as e]
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

(defn- control-data [left top]
 {:left (- left 8)
  :top (- top 8)
  :width 16
  :height 16
  :background-color "white"
  :border-color "black"
  :visible false})

(defn- relation-data [points]
 (let [p1 (first points)
       p2 (last points)]
   {:x1  (first p1) :y1  (last p1)
    :x2  (first p2) :y2  (last p2)
    :left (first p1) :top (last p1)}))

(defn- endpoint-data [point visible]
 {:left (- (first point) 8)
  :top (- (last point)   8)
  :radius 8
  :width 16
  :height 16
  :background-color "white"
  :border-color "black"
  :visible visible})

(defn- arrow-data [point]
  {:left (first point)
   :top  (last point)
   :origin-x :center
   :origin-y :center
   :angle 90
   :width 20
   :height 20})

(defentity rectangle-node
  (with-layouts
    (layout :attributes l/default-flow-layout #(-> % :attributes vals) {:left 15 :top 15}))
  (with-components data options
      [#(c/main % "body"  {:round-x 5 :round-y 5} {})
       #(c/control % "connector-left" {} {:side :left})
       #(c/control % "connector-right" {} {:side :right})
       #(c/control % "connector-top" {} {:side :top})
       #(c/control % "connector-bottom" {} {:side :bottom})])
  (with-attributes [#(stdatr/name % "<Enter name here>")
                    #(stdatr/description % "<Enter descrition here>")
                    #(stdatr/state % :open)]))

(defn- relation-layout-options [e]
  (let [bbox (cl/get-bbox e)]
    {:left (/ (:width bbox) 2)
     :top  (/ (:height bbox) 2)}))

(defentity relation
  (with-layouts
    (layout :attributes l/default-flow-layout #(-> % :attributes vals) relation-layout-options))
  (with-components data options
     [#(c/relation % "connector" {} {:start "start" :end "end"})
      #(c/startpoint % "start" {} {})
      #(c/arrow % "arrow" {} {})
      #(c/endpoint % "end" {} {})])
  (with-attributes [#(stdatr/name % "<< Relation Name >>")]))
