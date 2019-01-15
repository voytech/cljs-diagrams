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

(defn- control-initializer [size]
  (fn [container props]
     {:width size
      :height size
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
    :stroke-style :dashed
    :border-width 1
    :z-index 0}))

(defn- endpoint-initializer [type visible]
  (fn [container props]
     {:left (- (if (= :start type) 0 (-> container :size :width)) 5)
      :top (- 5)
      :width 10
      :z-index :top
      :height 10
      :background-color "white"
      :border-color "black"
      :visible visible}))

(defn- arrow-initializer []
  (fn [container props]
    {:left (-> container :size :width)
     :top  0
     :origin-x :center
     :origin-y :center
     :angle 0
     :width 20
     :z-index :before-top
     :border-color "black"
     :border-style :solid
     :border-width 3
     :background-color "none"
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
     :background-color "none"
     :stroke-style :solid
     :border-width 1
     :z-index 0}))

(defcomponent relation {:rendering-method :draw-poly-line
                        :initializer (poly-line-initializer)})

(defn triangle-bbox-draw []
  (fn [component]
    (let [x (d/get-left component)
          y (d/get-top component)
          width (d/get-width component)
          height (d/get-height component)]
     {:angle (d/getp component :angle)
      :points [x y
               (+ x width) (+ y (/ height 2))
               x (+ y height)]})))

(defcomponent arrow {:rendering-method :draw-poly-line
                     :bbox-draw (triangle-bbox-draw)
                     :initializer (arrow-initializer)})

(defcomponent startpoint {:rendering-method :draw-rect
                          :initializer (endpoint-initializer :start true)})

(defcomponent endpoint {:rendering-method :draw-rect
                        :initializer (endpoint-initializer :end false)})

(defcomponent breakpoint {:rendering-method :draw-circle
                          :initializer (fn [e] {:moveable true :visible true :opacity 1 :z-index :top})})

(defcomponent control {:rendering-method :draw-rect
                       :initializer (control-initializer 16)})

(defcomponent entity-shape {:rendering-method :draw-rect
                            :initializer (entity-shape-initializer)})

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

(defcomponent title {:rendering-method :draw-text
                     :initializer (title-initializer)})

(defn- image-initializer [width height]
  (fn [container props]
    {:border-color "black"
     :border-style :solid
     :border-width 1
     :width width
     :height height}))

(defcomponent image {:rendering-method :draw-image
                     :initializer (image-initializer 50 50)})

(defcomponent text {:rendering-method :draw-text
                    :initializer (fn [c p] {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})})

(defcomponent description {:rendering-method :draw-text
                           :initializer (fn [c p] {:border-color "black" :border-style :solid :border-width 1 :font-family "calibri" :font-size 12})})

(defcomponent rectangle {:rendering-method :draw-rect
                         :initializer (fn [c p] {:border-color "black" :border-style :solid :border-width 1})})

(defcomponent bounding-box {:rendering-method :draw-rect
                            :initializer (fn [c p] {:border-color "gray" :stroke-style :dashed :opacity 0.1 :border-width 1 :visible false})})

(defcomponent-group entity-controls
  (component control {:name  "connector-left"
                      :attributes {:side :left}
                      :layout-attributes (layout-attributes ::w/expression  (layout-hints (weighted-position 0 0.5) (weighted-origin 0.5 0.5)))})
  (component control {:name  "connector-right"
                      :attributes {:side :right}
                      :layout-attributes (layout-attributes ::w/expression (layout-hints (weighted-position 1 0.5) (weighted-origin 0.5 0.5)))})
  (component control {:name  "connector-top"
                      :attributes {:side :top}
                      :layout-attributes (layout-attributes ::w/expression (layout-hints (weighted-position 0.5 0) (weighted-origin 0.5 0.5)))})
  (component control {:name "connector-bottom"
                      :attributes {:side :bottom}
                      :layout-attributes (layout-attributes ::w/expression (layout-hints (weighted-position 0.5 1) (weighted-origin 0.5 0.5)))}))

(defcomponent-group small-controls
  (component control {:name  "connector-left"
                      :attributes {:side :left}
                      :model {:width 10 :height 10}
                      :layout-attributes (layout-attributes ::w/expression  (layout-hints (weighted-position 0 0.5) (weighted-origin 0.5 0.5)))})
  (component control {:name  "connector-right"
                      :attributes {:side :right}
                      :model {:width 10 :height 10}
                      :layout-attributes (layout-attributes ::w/expression (layout-hints (weighted-position 1 0.5) (weighted-origin 0.5 0.5)))})
  (component control {:name  "connector-top"
                      :attributes {:side :top}
                      :model {:width 10 :height 10}
                      :layout-attributes (layout-attributes ::w/expression (layout-hints (weighted-position 0.5 0) (weighted-origin 0.5 0.5)))})
  (component control {:name "connector-bottom"
                      :attributes {:side :bottom}
                      :model {:width 10 :height 10}
                      :layout-attributes (layout-attributes ::w/expression (layout-hints (weighted-position 0.5 1) (weighted-origin 0.5 0.5)))}))
