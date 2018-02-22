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

(defn- dimmension [drawable d2 d1]
  (- (d/getp drawable d2)
     (d/getp drawable d1)))

(defn- manage-related-properties [drawable p]
  (d/suppress-hook ::relation :setp ;; prevent circular dependency invocations
    (fn []
      (cond
        (= p :left) (let [width (dimmension drawable :x2 :x1)]
                      (d/setp drawable :x1 (d/getp drawable :left))
                      (d/setp drawable :x2 (+ (d/getp drawable :left) width)))
        (= p :top)  (let [height (dimmension drawable :y2 :y1)]
                      (d/setp drawable :y1 (d/getp drawable :top))
                      (d/setp drawable :y2 (+ (d/getp drawable :top) height)))
        :else (do (setup-bbox drawable p :x1 :x2 :left :width)
                  (setup-bbox drawable p :y1 :y2 :top  :height))))))

(d/add-hook ::relation :setp manage-related-properties)

(d/add-hook ::relation :set-data (fn [drawable data]
                                     (doseq [p (keys data)]
                                      (manage-related-properties drawable p))))

(defcomponent relation :draw-line {} {:border-color "black" :border-style :solid :border-width 1 :z-index :bottom})

(defcomponent arrow :draw-triangle {} {:border-color "black" :border-style :solid :border-width 1 :width 20 :height 20 :angle 90 :origin-x :center :origin-y :center})

(defcomponent startpoint :draw-circle {:start "connector" :penultimate true} {:moveable true :visible true :z-index :top})

(defcomponent endpoint :draw-circle {:end "connector"} {:moveable true :visible false :opacity 1 :z-index :top})

(defcomponent breakpoint :draw-circle {} {:moveable true :visible true :opacity 1 :z-index :top})

(defcomponent control :draw-rect {} {:moveable false :visible false :opacity 1 :background-color "white"})

(defcomponent main :draw-rect {} {:border-color "black" :border-style :solid :border-width 1})

;; Attribute components.

(defcomponent label :draw-text {} {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})

(defcomponent text :draw-text {} {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})

(defcomponent description :draw-text {} {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})

(defcomponent rect :draw-rect {} {:border-color "black" :border-style :solid :border-width 1})
