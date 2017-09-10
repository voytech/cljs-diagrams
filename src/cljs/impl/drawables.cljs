(ns impl.drawables
  (:require [core.drawables :as drawables])
  (:require-macros [core.macros :refer [defdrawable]]))

(defn- setup-bbox [drawable p p1 p2 p3 p4]
  (when (or (= p p1) (= p p2))
    (if (>= (drawables/getp drawable p1) (drawables/getp drawable p2))
       (do (drawables/setp drawable p3 (drawables/getp drawable p2))
           (drawables/setp drawable p4 (- (drawables/getp drawable p1) (drawables/getp drawable p2))))
       (do (drawables/setp drawable p3 (drawables/getp drawable p1))
           (drawables/setp drawable p4 (- (drawables/getp drawable p2) (drawables/getp drawable p1)))))))

(drawables/add-hook :line :setp (fn [drawable p] (setup-bbox drawable p :x1 :x2 :left :width)
                                                 (setup-bbox drawable p :y1 :y2 :top :height)))

(drawables/add-hook :line :set-data (fn [drawable data]
                                      (doseq [p (keys data)]
                                       (setup-bbox drawable p :x1 :x2 :left :width)
                                       (setup-bbox drawable p :y1 :y2 :top :height))))


(defdrawable rect {:border-color "black" :border-style :solid :border-width 1})

(defdrawable circle {:border-color "black" :border-style :solid :border-width 1})

(defdrawable ellipse {:border-color "black" :border-style :solid :border-width 1})

(defdrawable line {:border-color "black" :border-style :solid :border-width 1})

(defdrawable dashed-line  {:border-color "black" :border-style :dashed :border-width 1})

(defdrawable dotted-line  {:border-color "black" :border-style :dotted :border-width 1})

(defdrawable triangle {:border-color "black" :border-style :solid :border-width 1})

(defdrawable point {:border-color "black" :border-style :solid :border-width 1})

(defdrawable path {:border-color "black" :border-style :solid :border-width 1})

(defdrawable text {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})

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
                        :background-color "white"
                        :border-color "black"
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
  (line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :left x1 :top y1 :width (+ x1 x2) :height (+ y1 y2)}))
