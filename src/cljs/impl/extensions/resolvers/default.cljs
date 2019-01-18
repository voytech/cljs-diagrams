(ns impl.extensions.resolvers.default
 (:require [core.entities :as e]
           [impl.components :as c]
           [impl.behaviours.manhattan :as m]
           [core.behaviour-api :as api]
           [impl.behaviours.behaviour-api :as stdapi]
           [impl.features.default :as f]
           [clojure.spec.alpha :as spec]
           [core.eventbus :as bus]
           [extensions.data-resolvers :as r])
 (:require-macros [extensions.macros :refer [defresolver]]))

(defn initialize [app-state]
  (r/register app-state
              ::set-title
              f/is-shape-entity
              (spec/keys :req-un [::title])
              (fn [app-state entity data]
                (e/assert-component c/title app-state entity "title" {:text (:title data)})
                (bus/fire app-state "uncommited.render")))

  (r/register app-state
              ::make-association
              f/is-association-entity
              (spec/keys :req-un [::x1 ::y1 ::x2 ::y2])
              (fn [app-state entity data]
                (m/set-relation-endpoints app-state entity {:x (+ (:x1 data) (:left data))
                                                            :y (+ (:y1 data) (:top data))}
                                                           {:x (+ (:left data) (:x2 data))
                                                            :y (+ (:top data) (:y2 data))})
                (let [startpoint (first (e/get-entity-component entity ::c/startpoint))]
                  (api/collides? app-state
                                 startpoint
                                 f/is-shape-entity
                                 (fn [src trg]
                                    (e/connect-entities app-state (:entity trg) (:entity src) :start)
                                    (stdapi/toggle-controls (:entity trg) false)
                                    (m/refresh-manhattan-layout app-state entity))
                                 (fn [src])))
                (bus/fire app-state "uncommited.render"))))
