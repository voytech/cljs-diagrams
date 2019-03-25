(ns cljs-diagrams.impl.layouts.expression
  (:require [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.eventbus :as b]
            [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.funcreg :as fr :refer [provide]]
            [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.layouts :as l])
  (:require-macros [cljs-diagrams.core.macros :refer [defp]]))

(spec/def ::hints (spec/keys :req-un [::position
                                      ::size
                                      ::origin]))
(defn bound-expression [expr]
  {:type :exp :value expr})

(defn size-expression [width-exp height-exp]
  (l/size  {:type :exp :value width-exp}
           {:type :exp :value height-exp}))

(defn position-expression [left-exp top-exp]
  (l/position  {:type :exp :value left-exp}
               {:type :exp :value top-exp}))

(defp width-of [shape-name margin]
 (fn [node layout]
   (let [shape (e/get-node-shape node shape-name)]
     (+ (d/get-width shape) (or margin 0)))))

(defp height-of [shape-name margin]
 (fn [node layout]
   (let [shape (e/get-node-shape node shape-name)]
     (+ (d/get-height shape) (or margin 0)))))

(defp left-of [shape-name margin]
 (fn [node layout]
   (let [shape (e/get-node-shape node shape-name)]
     (- (d/get-left shape) (or margin 0)))))

(defp top-of [shape-name margin]
 (fn [node layout]
   (let [shape (e/get-node-shape node shape-name)]
     (- (d/get-top shape) (or margin 0)))))

(defn size-of
  ([shape-name margin-x margin-y]
   (size-expression (width-of shape-name margin-x)
                    (height-of shape-name margin-y)))
  ([shape-name] (size-of shape-name 0 0)))

(defn position-of
  ([shape-name margin-x margin-y]
   (position-expression (left-of shape-name margin-x)
                        (top-of shape-name margin-y)))
  ([shape-name]
   (position-of shape-name 0 0)))

(defn layout-hints
  ([position size origin]
   {:position position :size size :origin origin})
  ([position origin]
   (layout-hints position nil origin))
  ([position]
   (layout-hints position nil (l/weighted-origin 0 0))))

(defn obtain-shape-bbox [node shape context]
  (let [bbox (:bbox node)
        hints (-> shape :layout-attributes :layout-hints)
        layout-ref (-> shape :layout-attributes :layout-ref)
        {:keys [position size origin]} hints
        {:keys [left top]} position
        {:keys [width height]} size
        {:keys [orig-x orig-y]} origin]
    (if-let [layout (get-in node [:layouts layout-ref])]
      (let [layout-pos  (l/absolute-position-of-layout node layout)
            layout-size (l/absolute-size-of-layout node layout)
            effective-width (cond
                               (= :wei (-> width :type)) (* (:width layout-size) (:value width))
                               (= :abs (-> width :type)) (:value width)
                               (= :exp (-> width :type)) ((provide (:value width)) node layout)
                               :else (d/get-width shape))
            effective-height (cond
                                (= :wei (-> height :type)) (* (:height layout-size) (:value height))
                                (= :abs (-> height :type)) (:value height)
                                (= :exp (-> height :type)) ((provide (:value height)) node layout)
                                :else (d/get-height shape))
            origin-offset-x (cond
                               (= :wei (-> orig-x :type)) (* effective-width (:value orig-x))
                               (= :abs (-> orig-x :type)) (:value orig-x)
                               (= :exp (-> orig-x :type)) ((provide (:value orig-x)) node layout)
                               :else 0)
            origin-offset-y (cond
                               (= :wei (-> orig-y :type)) (* effective-height (:value orig-y))
                               (= :abs (-> orig-y :type)) (:value orig-y)
                               (= :exp (-> orig-y :type)) ((provide (:value orig-y)) node layout)
                               :else 0)]
          {:processing-context context
           :to-set
              {:width effective-width
               :height effective-height
               :left  (- (cond
                            (= :wei (-> left :type)) (+ (:left layout-pos) (* (:width layout-size) (:value left)))
                            (= :rel (-> left :type)) (+ (:left layout-pos ) (:value left))
                            (= :abs (-> left :type)) (:value left)
                            (= :exp (-> left :type)) ((provide (:value left)) node layout bounds)
                            :else (d/get-left shape)) origin-offset-x)
               :top  (- (cond
                            (= :wei (-> top :type)) (+ (:top layout-pos) (* (:height layout-size) (:value top)))
                            (= :rel (-> top :type)) (+ (:top layout-pos ) (:value top))
                            (= :abs (-> top :type)) (:value top)
                            (= :exp (-> top :type)) ((provide (:value top)) node layout bounds)
                            :else (d/get-left shape)) origin-offset-y)}}))))

(defmethod l/create-context ::expression [app-state node layout] {})

(defmethod l/layout-function ::expression [node shape context]
  (obtain-shape-bbox node shape context))
