(ns impl.behaviours.definitions
  (:require [core.behaviours :as b]
            [core.eventbus :as bus]
            [core.options :as o]
            [core.entities :as e]
            [core.events :as ev]
            [core.drawables :as d]
            [impl.behaviours.standard-api :as std]
            [impl.behaviours.manhattan :as m]
            [impl.components :as c])
  (:require-macros [core.macros :refer [defbehaviour having-all having-strict make-event validate bind-to --]]))


(defbehaviour moving-entity
              "Default Entity Moving" :moving
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                "mousedrag")
              (fn [e]
                (let [event (:context @e)]
                  ((std/moving-entity) event)
                  nil)))

(defbehaviour moving-entity-by
              "Default Relation Link Moving By" :moving-by
              (validate
                (-- (having-all ::c/startpoint ::c/endpoint ::c/relation))
                (fn [entity behaviour result] (ev/loose-event-name (:type entity) nil nil "moveby")))
              (fn [e]
                (let [event (:context @e)
                      entity (:entity event)
                      relation (:relation event)
                      adata (:association-data relation)
                      component (e/get-entity-component entity adata)
                      enriched (merge event {:component component :drawable (:drawable component)})]
                  ((m/manhattan-layout-moving-behaviour) enriched)
                  nil)))

(defbehaviour manhattan-moving-component
              "Manhattan Layout" :component-moving
              (validate
                (-- (having-all ::c/startpoint ::c/endpoint ::c/relation)
                    (bind-to ::c/startpoint ::c/endpoint))
                "mousedrag")
              (fn [e]
                (let [event (:context @e)]
                  ((m/manhattan-layout-moving-behaviour) event)
                  ((std/intersects? "body" (fn [src trg] (std/toggle-controls (:entity trg) true))
                                           (fn [src trg] (std/toggle-controls (:entity trg) false))) (:context @e))
                  nil)))

;(defbehaviour free-moving-component
;              "Moving Entity Component" :component-moving
;              (b/generic-validator [{:tmpl #{:startpoint :endpoint :relation}
;                                     :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
;                                     :result [:startpoint :endpoint]))
;              "mousedrag"
;              (fn [e]
;                (let [event (:context @e)]
;                  ((ib/moving-endpoint) event)
;                  ((b/intersects? "body" (fn [src trg] (ib/toggle-controls (:entity trg) true))
;                                         (fn [src trg] (ib/toggle-controls (:entity trg) false))) (:context @e)
;                  (bus/fire "uncommited.render")   ;TODO Fire on after all handlers executed.
;                  (bus/fire "rendering.finish") ;TODO Fire on after all handlers executed.
;                  nil)))

(defbehaviour make-relation
              "Connect Two Entities" :make-relation
              (validate
                (-- (having-all ::c/startpoint ::c/endpoint ::c/relation)
                    (bind-to ::c/startpoint ::c/endpoint))
                "mouseup")
              (fn [e]
                (let [event (:context @e)]
                  ((std/intersects-controls? (fn [src trg]
                                               (let [ctype (-> event :component :type)
                                                     end-type (cond
                                                                (= ::c/endpoint ctype) {:type "end" :f std/position-endpoint}
                                                                (= ::c/startpoint ctype) {:type  "start" :f std/position-startpoint})]
                                                (e/connect-entities (:entity src) (:entity trg) :entity-link (:type end-type) (:type end-type))
                                                (std/toggle-controls (:entity trg) false)
                                                (let [connector-side (e/component-property (:entity trg) (:name (:component trg)) :side)]
                                                  (e/update-component-prop (:entity src) (:type end-type) :rel-connector connector-side)
                                                  (e/update-component-prop (:entity src) (:type end-type) :rel-entity-uid (-> trg :entity :uid)))
                                                ((:f end-type) (:entity src) (d/get-left (:drawable trg)) (d/get-top (:drawable trg)))))) (:context @e))
                  (std/relations-validate (->> @e :context :entity))
                  nil)))

(defbehaviour hovering-entity
              "Default Entity Hovering" :hovering
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                (-- (having-all ::c/startpoint ::c/endpoint)
                    (bind-to ::c/startpoint ::c/endpoint))
                "mousemove")
              (fn [e]
                (let [event (:context @e)]
                  ((std/highlight true o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  nil)))

(defbehaviour leaving-entity
              "Default Entity Leave" :leaving
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                (-- (having-all ::c/startpoint ::c/endpoint)
                    (bind-to ::c/startpoint ::c/endpoint))
                "mouseout")
              (fn [e]
                (let [event (:context @e)]
                  ((std/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  nil)))

(defbehaviour show-entity-controls
              "Default Show Controls" :controls-show
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                "mousemove")
              (fn [e]
                (let [event (:context @e)]
                  (std/toggle-controls (:entity event) true)
                  nil)))

(defbehaviour hide-entity-controls
              "Default Hide Controls" :controls-hide
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                "mouseout")
              (fn [e]
                (let [event (:context @e)]
                  (std/toggle-controls (:entity event) false)
                  nil)))


; (bus/on ["relation.relation.mousepointclick"] -999 (fn [e]
;                                                     ((insert-breakpoint) (:context @e))
;                                                     (bus/fire "uncommited.render")
;                                                     (bus/fire "rendering.finish")))
;
; (bus/on ["relation.breakpoint.mousepointclick"] -999 (fn [e]
;                                                       ((dissoc-breakpoint) (:context @e))
;                                                       (bus/fire "uncommited.render")
;                                                       (bus/fire "rendering.finish")))
