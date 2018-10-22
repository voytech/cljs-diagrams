(ns impl.renderers.svg
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]
            [core.rendering :as r]
            [core.utils.dom :as dom]
            [impl.components :as impld]))

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :fabric [dom-id width height]
  (let [svg-container (dom/by-id dom-id)]
     svg-container))

(defmethod r/all-rendered :fabric [context]
  (.renderAll (get context :canvas)))
