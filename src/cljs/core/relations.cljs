(ns core.relations
  (:require [core.eventbus :as bus]
            [core.components :as d]
            [core.state :as state]
            [core.entities :as e]
            [core.utils.general :as utils :refer [make-js-property]]))

(defrecord Relationship [type
                         validator
                         source-setup-behaviour
                         target-setup-behaviour])

(defn define-relationship [app-state type source-validator target-validator source-setup target-setup]
  (let [relationship (Relationship. type
                                    source-validator
                                    target-validator
                                    source-setup
                                    target-setup)]
      (swap! app-state assoc-in [:diagram :relationships type] relationship)))

(defn relationships [app-state]
  (vals (get-in @app-state [:diagram :relationships])))

(defn test-relationships [app-state source-entity target-entity]
  (doseq [relationship (relationships app-state)
          validator (:validator relationship)]
    (when (validator source-entity target-entity)
        (let [s-setup (:source-setup-behaviour relationship)
              t-setup (:target-setup-behaviour relationship)]
            (e/connect-entities app-state source-entity target-entity (:type relationship))
            (s-setup app-state source-entity target-entity)
            (t-setup app-state target-entity source-entity)))))

(defn test-relationship [app-state entity relationship])
