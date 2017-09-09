(ns impl.behaviours.definitions
  (:require [core.behaviours :as b]
            [core.eventbus :as bus]
            [core.options :as o]
            [core.entities :as e]
            [core.drawables :as d]
            [impl.behaviours.standard :as ib])
  (:require-macros [core.macros :refer [defbehaviour]]))

(defbehaviour moving-entity
              "Default Entity Moving" :moving
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}])
              "mousedrag"
              (fn [e]
                (let [event (:context @e)]
                  ((b/moving-entity) event)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.

(defbehaviour moving-component
              "Moving Entity Component" :component-moving
              (b/generic-validator [{:tmpl #{:startpoint :endpoint :relation}
                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                     :result [:startpoint :endpoint]}])
              "mousedrag"
              (fn [e]
                (let [event (:context @e)]
                  ((ib/moving-endpoint) event)
                  ((b/intersects? "body" (fn [src trg] (ib/toggle-endpoints (:entity trg) true))
                                         (fn [src trg] (ib/toggle-endpoints (:entity trg) false))) (:context @e))
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.

(defbehaviour make-relation
              "Connect Two Entities" :make-relation
              (b/generic-validator [{:tmpl #{:startpoint :endpoint :relation}
                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                     :result [:startpoint :endpoint]}])
              "mouseup"
              (fn [e]
                (let [event (:context @e)]
                  ((b/intersects-endpoints? (fn [src trg]
                                              (e/connect-entities (:entity src) (:entity trg) :entity-link "start" "start")
                                              (ib/toggle-endpoints (:entity trg) false)
                                              (ib/position-startpoint (:entity src) (d/get-left (:drawable trg)) (d/get-top (:drawable trg))))) (:context @e))
                  (b/relations-validate (->> @e :context :entity))
                  (bus/fire "uncommited.render")
                  (bus/fire "rendering.finish"))))

(defbehaviour hovering-entity
              "Default Entity Hovering" :hovering
              ;{:after [:moving]}
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}
                                    {:tmpl #{:startpoint :endpoint}
                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                     :result [:startpoint :endpoint]}])
              "mousemove"
              (fn [e]
                (let [event (:context @e)]
                  ((b/highlight true o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.

(defbehaviour leaving-entity
              "Default Entity Leave" :leaving
              ;{:after [:moving]}
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}
                                    {:tmpl #{:startpoint :endpoint}
                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                     :result [:startpoint :endpoint]}])
              "mouseout"
              (fn [e]
                (let [event (:context @e)]
                  ((b/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.

(defbehaviour show-entity-controls
              "Default Show Controls" :controls-show
              ;{:after [:hovering]}
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}])
              "mousemove"
              (fn [e]
                (let [event (:context @e)]
                  (ib/toggle-endpoints (:entity event) true)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.

(defbehaviour hide-entity-controls
              "Default Hide Controls" :controls-hide
              ;{:after [:leaving]}
              (b/generic-validator [{:tmpl #{:main :endpoint}
                                     :func (fn [requires types] (= requires types))
                                     :result [:main]}])
              "mousemove"
              (fn [e]
                (let [event (:context @e)]
                  (ib/toggle-endpoints (:entity event) false)
                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
                  (bus/fire "rendering.finish")))) ;TODO Fire on after all handlers executed.
