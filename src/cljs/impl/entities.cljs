(ns impl.entities
 (:require [core.entities :as e]
           [core.options :as defaults]
           [impl.standard-attributes :as stdatr]
           [impl.drawables :as d]
           [impl.components :as c]
           [core.drawables :as cd]
           [core.behaviours :as cb]
           [impl.behaviours.standard :as ib]
           [core.project :as p]
           [core.eventbus :as bus]
           [core.options :as o]
           [impl.behaviours.definitions :as bd]
           [clojure.string :as str])
 (:require-macros [core.macros :refer [defentity with-components]]))

(defentity rectangle-node
  (with-content-bounding-box {:left 15
                              :top  15
                              :width  175
                              :height 150})
  (with-behaviours {})
  (with-components data options
    (let [enriched-opts (merge options defaults/DEFAULT_SIZE_OPTS defaults/TRANSPARENT_FILL defaults/DEFAULT_STROKE)
          conL    (vector (:left options) (+ (/ (:height defaults/DEFAULT_SIZE_OPTS) 2) (:top options)))
          conR    (vector (+ (:left options) (:width defaults/DEFAULT_SIZE_OPTS)) (+ (/ (:height defaults/DEFAULT_SIZE_OPTS) 2) (:top options)))
          conT    (vector (+ (/ (:width defaults/DEFAULT_SIZE_OPTS) 2) (:left options)) (:top options))
          conB    (vector (+ (/ (:width defaults/DEFAULT_SIZE_OPTS) 2) (:left options)) (+ (:top options) (:height defaults/DEFAULT_SIZE_OPTS)))]
      [(c/control "connector-left"   {:point conL} {:side :left})
       (c/control "connector-right"  {:point conR} {:side :right})
       (c/control "connector-top"    {:point conT} {:side :top})
       (c/control "connector-bottom" {:point conB} {:side :bottom})
       (c/main "body" enriched-opts)]))
  (with-attributes [#(stdatr/name % "<Enter name here>")
                    #(stdatr/description % "<Enter descrition here>")
                    #(stdatr/state % :open)]))

(defentity relation
  (with-content-bounding-box {:left 15
                              :top  15
                              :width  180
                              :height 150})
  (with-behaviours {})
  (with-components data options
    (let [enriched-opts options
          offset-x (:left options)
          offset-y (:top options)
          points-pairs (partition 2 data)
          points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
          conS (first points-pairs-offset)
          conE (last points-pairs-offset)]
        [(c/relation "connector" {:x1 (first conS) :y1 (last conS) :x2 (first conE) :y2 (last conE)} {:start "start" :end "end"})
         (c/startpoint "start" {:point conS})
         (c/arrow "arrow" {:data data :options options})
         (c/endpoint "end" {:point conE :opacity 0})])))

(cb/set-relation-movement-hook "rectangle-node" "relation" (fn [source target relation left top coord-mode]
                                                             (let [adata (:association-data relation)
                                                                   target-component (e/get-entity-component target adata)]
                                                               (cond
                                                                 (= :startpoint (:type target-component)) (ib/position-startpoint target left top :offset)
                                                                 (= :endpoint   (:type target-component)) (ib/position-endpoint   target left top :offset)))))
