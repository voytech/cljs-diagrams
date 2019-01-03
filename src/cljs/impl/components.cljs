(ns impl.components
  (:require [core.components :as d :refer [layout-attributes]]
            [impl.layouts.expression :as w :refer [layout-hints]]
            [core.layouts :as l :refer [layout
                                        weighted-position
                                        weighted-size
                                        weighted-origin
                                        match-parent-size
                                        match-parent-position
                                        margins]])
  (:require-macros [core.macros :refer [defcomponent defcomponent-group component]]))

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
    :z-index 0}))

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

(defn poly-line-initializer []
  (fn [container props]
    {:points  [0 0 (-> container :bbox :width) 0]
     :left 0
     :top 0
     :border-color "black"
     :border-style :solid
     :border-width 1
     :z-index 0}))

;(defcomponent relation :draw-line {} (relation-initializer))

(defcomponent relation :draw-poly-line {} (poly-line-initializer))

(defcomponent arrow :draw-triangle {} (arrow-initializer))

(defcomponent startpoint :draw-circle {:start "connector" :penultimate true} (endpoint-initializer :start true))

(defcomponent endpoint :draw-circle {:end "connector"} (endpoint-initializer :end false))

(defcomponent breakpoint :draw-circle {} (fn [e] {:moveable true :visible true :opacity 1 :z-index :top}))

(defcomponent control :draw-rect {} (control-initializer))

(defcomponent entity-shape :draw-rect {} (entity-shape-initializer))

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

(defcomponent title :draw-text {} (title-initializer))

(defn- image-initializer [width height]
  (fn [container props]
    {:border-color "black"
     :border-style :solid
     :border-width 1
     :width width
     :height height}))

(defcomponent image :draw-image {} (image-initializer 50 50))

(defcomponent text :draw-text {} (fn [c p] {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12}))

(defcomponent description :draw-text {} (fn [c p] {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12}))

(defcomponent rectangle :draw-rect {} (fn [c p] {:border-color "black" :border-style :solid :border-width 1}))

(defcomponent bounding-box :draw-rect {} (fn [c p] {:border-color "lightgray" :border-style :dotted :opacity 0.1 :border-width 1 :visible false}))

(defcomponent-group entity-controls
  (component control "connector-left" {} {:side :left}
    (layout-attributes ::w/expression  (layout-hints (weighted-position 0 0.5) (weighted-origin 0.5 0.5))))
  (component control "connector-right" {} {:side :right}
    (layout-attributes ::w/expression (layout-hints (weighted-position 1 0.5) (weighted-origin 0.5 0.5))))
  (component control "connector-top" {} {:side :top}
    (layout-attributes ::w/expression (layout-hints (weighted-position 0.5 0) (weighted-origin 0.5 0.5))))
  (component control "connector-bottom" {} {:side :bottom}
    (layout-attributes ::w/expression (layout-hints (weighted-position 0.5 1) (weighted-origin 0.5 0.5)))))
