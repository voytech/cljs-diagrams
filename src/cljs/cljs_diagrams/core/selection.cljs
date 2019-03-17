(ns cljs-diagrams.core.selection
  (:require [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.state :as state]))

(defn select [app-state node]
  (state/assoc-diagram-state app-state [:selection] (:uid node)))

(defn get-selected-node [app-state]
  (let [uid (state/get-in-diagram-state app-state [:selection])]
    (e/node-by-id app-state uid)))
