(ns cljs-diagrams.core.selection
  (:require [cljs-diagrams.core.entities :as e]
            [cljs-diagrams.core.state :as state]))

(defn select [app-state entity]
  (state/assoc-diagram-state app-state [:selection] (:uid entity)))

(defn get-selected-entity [app-state]
  (let [uid (state/get-in-diagram-state app-state [:selection])]
    (e/entity-by-id app-state uid)))
