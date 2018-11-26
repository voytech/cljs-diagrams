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
