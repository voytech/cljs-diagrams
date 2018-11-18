(ns impl.behaviours.definitions
  (:require [core.behaviours :as b]
            [core.eventbus :as bus]
            [core.options :as o]
            [core.entities :as e]
            [core.events :as ev]
            [core.components :as d]
            [impl.behaviours.standard-api :as std]
            [impl.behaviours.manhattan :as m]
            [impl.components :as c])
  (:require-macros [core.macros :refer [defbehaviour having-all having-strict make-event validate bind-to --]]))

(defbehaviour moving-node-entity
              "Default Entity Moving" :node-moving
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                "move")
              (fn [e]
                (let [event (:context e)]
                  ((std/moving-entity) event)
                  nil)))

(defbehaviour moving-connector-entity-by
              "Default Relation Link Moving By" :moving-by
              (validate
                (-- (having-all ::c/startpoint ::c/endpoint ::c/relation))
                (fn [entity behaviour result] (ev/event-name (:type entity) nil nil "moveby")))
              (fn [e]
                (let [event (:context e)
                      entity (:entity event)
                      start (first (e/get-related-entities entity :start))
                      end (first (e/get-related-entities entity :end))
                      enriched (merge event {:start start :end end})]
                  (m/on-source-entity-event enriched)
                  nil)))

(defbehaviour moving-connector-endpoints
              "Connector's endpoints moving [Manhattan]" :connector-endpoint-moving
              (validate
                (-- (having-all ::c/startpoint ::c/endpoint ::c/relation)
                    (bind-to ::c/startpoint ::c/endpoint))
                "move")
              (fn [e]
                (let [event (:context e)]
                  (m/on-endpoint-event event)
                  ((std/intersects? "body" (fn [src trg] (std/toggle-controls (:entity trg) true))
                                           (fn [src trg] (std/toggle-controls (:entity trg) false))) (:context e))
                  nil)))

(defbehaviour make-relation
              "Connect Two Entities" :make-relation
              (validate
                (-- (having-all ::c/startpoint ::c/endpoint ::c/relation)
                    (bind-to ::c/startpoint ::c/endpoint))
                "mouse-up")
              (fn [e]
                (let [event (:context e)]
                  ((std/intersects-controls? (fn [src trg]
                                               (let [ctype (-> event :component :type)
                                                     end-type (cond
                                                                (= ::c/endpoint ctype) {:type "end" :f std/position-endpoint}
                                                                (= ::c/startpoint ctype) {:type  "start" :f std/position-startpoint})]
                                                (e/connect-entities (:entity src) (:entity trg) (keyword (:type end-type)))
                                                (std/toggle-controls (:entity trg) false)
                                                ((:f end-type) (:entity src) (d/get-left (:component trg)) (d/get-top (:component trg)))))) (:context e))
                  (std/relations-validate (->> e :context :entity))
                  (m/on-endpoint-event event)
                  nil)))

(defbehaviour hovering-entity
              "Default Entity Hovering" :hovering
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                (-- (having-all ::c/startpoint ::c/endpoint)
                    (bind-to ::c/startpoint ::c/endpoint))
                "focus")
              (fn [e]
                (let [event (:context e)]
                  ((std/highlight true o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  nil)))

(defbehaviour leaving-entity
              "Default Entity Leave" :leaving
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                (-- (having-all ::c/startpoint ::c/endpoint)
                    (bind-to ::c/startpoint ::c/endpoint))
                "blur")
              (fn [e]
                (let [event (:context e)]
                  ((std/highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                  nil)))

(defbehaviour show-all-entity-controls
              "Default Show Controls" :controls-show
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                "focus")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-controls (:entity event) true)
                  nil)))

(defbehaviour hide-all-entity-controls
              "Default Hide Controls" :controls-hide
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/main))
                "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-controls (:entity event) false)
                  nil)))

(defbehaviour show-entity-control
              "Default Show Controls" :control-show
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/control))
                (-- (having-all ::c/relation ::c/control)
                    (bind-to ::c/control))
                "focus")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-control (:entity event) (-> event :component :name) true)
                  nil)))

(defbehaviour hide-entity-control
              "Default Hide Controls" :control-hide
              (validate
                (-- (having-strict ::c/main ::c/control)
                    (bind-to ::c/control))
                (-- (having-all ::c/relation ::c/control)
                    (bind-to ::c/control))
                "blur")
              (fn [e]
                (let [event (:context e)]
                  (std/toggle-control (:entity event) (-> event :component :name) false)
                  nil)))
