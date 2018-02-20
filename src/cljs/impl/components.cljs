(ns impl.components
  (:require [core.entities :as e]
            [core.components :as d])
  (:require-macros [core.macros :refer [defcomponent]]))


(defn- setup-bbox [drawable p p1 p2 p3 p4]
  (when (or (= p p1) (= p p2))
    (if (>= (d/getp drawable p1) (d/getp drawable p2))
       (do (d/setp drawable p3 (d/getp drawable p2))
           (d/setp drawable p4 (- (d/getp drawable p1) (d/getp drawable p2))))
       (do (d/setp drawable p3 (d/getp drawable p1))
           (d/setp drawable p4 (- (d/getp drawable p2) (d/getp drawable p1)))))))

(d/add-hook ::relation :setp (fn [drawable p] (setup-bbox drawable p :x1 :x2 :left :width)
                                              (setup-bbox drawable p :y1 :y2 :top :height)))

(d/add-hook ::relation :set-data (fn [drawable data]
                                     (doseq [p (keys data)]
                                      (setup-bbox drawable p :x1 :x2 :left :width)
                                      (setup-bbox drawable p :y1 :y2 :top :height))))

(defcomponent relation :default {} {:border-color "black" :border-style :solid :border-width 1 :z-index 0})

(defcomponent arrow :default {} {:border-color "black" :border-style :solid :border-width 1 :width 20 :height 20 :angle 90 :origin-x :center :origin-y :center})

(defcomponent startpoint :default {:start "connector" :penultimate true} {:moveable true :visible true})

(defcomponent endpoint :default {:end "connector"} {:moveable true :visible false :opacity 1})

(defcomponent breakpoint :default {} {:moveable true :visible true :opacity 1})

(defcomponent control :default {} {:moveable false :visible false :opacity 1 :background-color "white"})

(defcomponent main :default {} {:border-color "black" :border-style :solid :border-width 1})

;; Attribute components.

(defcomponent value :default {} {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})

(defcomponent description :default {} {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})
