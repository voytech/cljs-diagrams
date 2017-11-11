(ns impl.entities
 (:require [core.entities :as e]
           [core.options :as defaults]
           [impl.standard-attributes :as stdatr]
           [impl.components :as c]
           [core.project :as p]
           [core.eventbus :as bus]
           [core.options :as o]
           [impl.behaviours.definitions :as bd]
           [clojure.string :as str])
 (:require-macros [core.macros :refer [defentity with-components]]))

(defn- control-data [point]
 {:left (- (first point) 8)
  :top (- (last point)   8)
  :width 16
  :height 16
  :background-color "white"
  :border-color "black"
  :visible false})

(defn- relation-data [conS conE]
 {:x1   (first conS)
  :y1   (last conS)
  :x2   (first conE)
  :y2   (last conE)
  :left (first conS)
  :top  (last conS)})
  ;:width  (+ (first conS) (first conE))
  ;:height (+ (last conS)  (last conE))})

(defn- endpoint-data [point visible]
 {:left (- (first point) 8)
  :top (- (last point)   8)
  :radius 8
  :background-color "white"
  :border-color "black"
  :visible visible})

(defn- arrow-data [data options]
  {:left (+ (:left options) (first (last (partition 2 data))))
   :top (+ (+ (:top options)) (- (+ (:top options)) (/ (+ (+ (:top options)) (+ (:top options)  (last (last (partition 2 data))))) 2)))
   :origin-x :center
   :origin-y :center
   :angle 90
   :width 20
   :height 20})


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
      [(c/control "connector-left"   (control-data conL) {:side :left})
       (c/control "connector-right"  (control-data conR) {:side :right})
       (c/control "connector-top"    (control-data conT) {:side :top})
       (c/control "connector-bottom" (control-data conB) {:side :bottom})
       (c/main "body" enriched-opts)]))
  (with-attributes [#(stdatr/name % "<Enter name here>")
                    #(stdatr/description % "<Enter descrition here>")
                    #(stdatr/state % :open)]))

;; todo:  consider positioning components relative to entity position
(defentity relation
  (with-content-bounding-box {:left 15
                              :top  15
                              :origin :center
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
        [(c/relation "connector" (relation-data conS conE) {:start "start" :end "end"})
         (c/startpoint "start" (endpoint-data conS true))
         (c/arrow "arrow" (arrow-data data options))
         (c/endpoint "end" (endpoint-data conE false))])))
