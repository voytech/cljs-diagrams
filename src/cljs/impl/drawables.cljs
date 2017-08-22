(ns impl.drawables
  (:require [core.drawables :as drawables])
  (:require-macros [core.macros :refer [defdrawable]]))

(defdrawable rect {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable circle {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable ellipse {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable line {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable dashed-line  {:z-index 0 :border-color "black" :border-style :dashed :border-width 1})

(defdrawable dotted-line  {:z-index 0 :border-color "black" :border-style :dotted :border-width 1})

(defdrawable triangle {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable point {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable path {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable text {:z-index 0 :border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})

(defmulti endpoint (fn [point & {:keys [moveable display visible]}] display))

(defmethod endpoint "circle" [point & {:keys [moveable display visible opacity]}]
  (let [options (merge {:left (- (first point) 8)
                        :top (- (last point)   8)
                        :radius 8
                        :background-color "white"
                        :border-color "black"
                        :visible visible
                        :opacity opacity})]

      (circle options)))

(defmethod endpoint "rect" [point & {:keys [moveable display visible]}]
  (let [options (merge {:left (- (first point) 8)
                        :top (- (last point)   8)
                        :width 16
                        :height 16
                        :visible visible})]
      (rect options)))

(defn arrow [data options]
  (let [x1 (+ (:left options))
        y1 (+ (:top options))
        x2 (+ (:left options) (first (last (partition 2 data))))
        y2 (+ (:top options)  (last  (last (partition 2 data))))
        cX (/ (+ x1 x2) 2)
        cY (/ (+ y1 y2) 2)
        deltaX (- x1 cX)
        deltaY (- y1 cY)]
      (triangle {:left x2
                 :top (+ y1 deltaY)
                 :origin-x :center
                 :origin-y :center
                 :angle 90
                 :width 20
                 :height 20})))

(defn relation-line [x1 y1 x2 y2]
  (line {:left x1 :top y1 :x1 x1 :y1 y1 :x2 x2 :y2 y2}))
