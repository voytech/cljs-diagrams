(ns impl.entities
 (:require [core.entities :as e]
           [core.options :as defaults]
           [impl.standard-attributes :as stdatr]
           [impl.drawables :as d]
           [core.drawables :as cd]
           [core.project :as p]
           [core.eventbus :as bus]
           [core.behaviours :as cb :refer [highlight show all event-wrap moving-entity intersects? intersects-endpoints? relations-validate]]
           [core.options :as o]
           [impl.behaviours.standard :as eb :refer [insert-breakpoint dissoc-breakpoint moving-endpoint
                                                    toggle-endpoints position-endpoint position-startpoint]]
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
      [{:name "connector-left"
        :type :endpoint
        :drawable (d/endpoint conL :moveable false :display "rect" :visibile false)}
       {:name "connector-right"
        :type :endpoint
        :drawable (d/endpoint conR :moveable false :display "rect" :visibile false)}
       {:name "connector-top"
        :type :endpoint
        :drawable (d/endpoint conT :moveable false :display "rect" :visibile false)}
       {:name "connector-bottom"
        :type :endpoint
        :drawable (d/endpoint conB :moveable false :display "rect" :visibile false)}
       {:name "body"
        :type :main
        :drawable (d/rect enriched-opts)}]))
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
        [{:name "connector"
          :type :relation
          :drawable  (d/relation-line (first conS) (last conS) (first conE) (last conE) enriched-opts)
          :props {:start "start" :end "end"}}

         {:name "start"
          :type :startpoint
          :drawable  (d/endpoint conS :moveable true :display "circle" :visible true :opacity 1)
          :props {:start "connector" :penultimate true}}

         {:name "arrow"
          :type :decorator
          :drawable  (d/arrow data options)}

         {:name "end"
          :type :endpoint
          :drawable  (d/endpoint conE :moveable true :display "circle" :visible true :opacity 0)
          :props {:end "connector"}}])))

(cb/set-relation-movement-hook "rectangle-node" "relation" (fn [source target relation left top coord-mode]
                                                             (let [adata (:association-data relation)
                                                                   target-component (e/get-entity-component target adata)]
                                                               (cond
                                                                 (= :startpoint (:type target-component)) (position-startpoint target left top :offset)
                                                                 (= :endpoint   (:type target-component)) (position-endpoint   target left top :offset)))))
