(ns impl.components
  (:require [core.components :as d]
            [core.layouts :as l :refer [layout
                                        relative-position
                                        relative-size
                                        relative-origin
                                        fill-size
                                        no-offset
                                        margins
                                        layout-hints]])
  (:require-macros [core.macros :refer [defcomponent named-group component]]))

(def WIDTH 180)
(def HEIGHT 150)

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

(defn- control-initializer []
  (fn [container props]
     {:width 16
      :height 16
      :opacity 1
      :background-color "white"
      :border-color "black"
      :visible true}))

(defn- relation-initializer []
 (fn [container props]
   {:x1  0
    :y1  0
    :x2 (-> container :size :width)
    :y2 0
    :left 0
    :top 0
    :border-color "black"
    :border-style :solid
    :border-width 1
    :z-index :before-bottom}))

(defn- endpoint-initializer [type visible]
  (fn [container props]
     {:left (- (if (= :start type) 0 (-> container :size :width)) 8)
      :top (- 8)
      :radius 8
      :width 16
      :z-index :top
      :height 16
      :background-color "white"
      :border-color "black"
      :visible visible}))

(defn- arrow-initializer []
  (fn [container props]
    {:left (-> container :size :width)
     :top  0
     :origin-x :center
     :origin-y :center
     :angle 90
     :width 20
     :z-index :top
     :height 20}))

(defn- entity-shape-initializer []
  (fn [container props]
    {:border-color "black"
     :border-style :solid
     :border-width 1
     :background-color "white"}))

(defcomponent relation :draw-line {} (relation-initializer) nil)

(defcomponent arrow :draw-triangle {} (arrow-initializer) nil)

(defcomponent startpoint :draw-circle {:start "connector" :penultimate true} (endpoint-initializer :start true) nil)

(defcomponent endpoint :draw-circle {:end "connector"} (endpoint-initializer :end false) nil)

(defcomponent breakpoint :draw-circle {} (fn [e] {:moveable true :visible true :opacity 1 :z-index :top}) nil)

(defcomponent control :draw-rect {} (control-initializer) nil)

(defcomponent entity-shape :draw-rect {} (entity-shape-initializer) (layout-hints (no-offset) (fill-size)))

;; ===================================
;; layout managed components.
;; ===================================
(defn- title-initializer []
  (fn [container props]
    {:border-color "black"
     :border-style :solid
     :border-width 1
     :font-family "calibri"
     :font-size 12}))

(defcomponent title :draw-text {:layout :attributes} (title-initializer) (layout-hints (relative-position 0.5 0.05) (relative-origin 0.5 0)))

(defn- image-initializer [width height]
  (fn [container props]
    {:border-color "black"
     :border-style :solid
     :border-width 1
     :width width
     :height height}))

(defcomponent image :draw-image {} (image-initializer 50 50) nil)

(defcomponent text :draw-text {} (fn [c] {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12}) nil)

(defcomponent description :draw-text {} (fn [c] {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12}) nil)

(defcomponent rect :draw-rect {} (fn [c] {:border-color "black" :border-style :solid :border-width 1}) nil)

(named-group entity-controls
  (component control "connector-left" {} {:side :left}
    (layout-hints (relative-position 0 0.5) (relative-origin 0.5 0.5)))
  (component control "connector-right" {} {:side :right}
    (layout-hints (relative-position 0 1) (relative-origin 0.5 0.5)))
  (component control "connector-top" {} {:side :top}
    (layout-hints (relative-position 0.5 0) (relative-origin 0.5 0.5)))
  (component control "connector-bottom" {} {:side :bottom}
    (layout-hints (relative-position 0.5 1) (relative-origin 0.5 0.5))))
