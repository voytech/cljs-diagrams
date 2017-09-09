(ns impl.behaviours.definitions
  (:require [core.behaviours :as b]
            [core.eventbus :as bus]
            [core.options :as o]
            [impl.behaviours.standard :as std])
  (:require-macros [core.macros :refer [defbehaviour]]))

(defbehaviour moving-entity
              "moving" "Default Entity Moving" :moving
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}
                                    {:tmpl #{:startpoint :endpoint :relation}
                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                     :result [:startpoint :endpoint]}])
              "mousedrag"
              (fn [e]
                (let [event (:context @e)]
                  ((b/moving-entity) event)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.

(defbehaviour hovering-entity
              "hovering" "Default Entity Hovering" :hovering
              ;{:after [:moving]}
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}
                                    {:tmpl #{:startpoint :endpoint :relation}
                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                     :result [:startpoint :endpoint]}])
              "mousemove"
              (fn [e]
                (let [event (:context @e)]
                  ((b/highlight true o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.

(defbehaviour leaving-entity
              "leaving" "Default Entity Leave" :leaving
              ;{:after [:moving]}
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}
                                    {:tmpl #{:startpoint :endpoint :relation}
                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                     :result [:startpoint :endpoint]}])
              "mouseout"
              (fn [e]
                (let [event (:context @e)]
                  ((b/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.
