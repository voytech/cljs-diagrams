(ns cljs-diagrams.impl.layouts.expression
  (:require [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.eventbus :as b]
            [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.funcreg :as fr :refer [provide]]
            [cljs-diagrams.core.entities :as e]
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

(defp width-of [component-name margin]
 (fn [entity layout]
   (let [component (e/get-entity-component entity component-name)]
     (+ (d/get-width component) (or margin 0)))))

(defp height-of [component-name margin]
 (fn [entity layout]
   (let [component (e/get-entity-component entity component-name)]
     (+ (d/get-height component) (or margin 0)))))

(defp left-of [component-name margin]
 (fn [entity layout]
   (let [component (e/get-entity-component entity component-name)]
     (- (d/get-left component) (or margin 0)))))

(defp top-of [component-name margin]
 (fn [entity layout]
   (let [component (e/get-entity-component entity component-name)]
     (- (d/get-top component) (or margin 0)))))

(defn size-of
  ([component-name margin-x margin-y]
   (size-expression (width-of component-name margin-x)
                    (height-of component-name margin-y)))
  ([component-name] (size-of component-name 0 0)))

(defn position-of
  ([component-name margin-x margin-y]
   (position-expression (left-of component-name margin-x)
                        (top-of component-name margin-y)))
  ([component-name]
   (position-of component-name 0 0)))

(defn layout-hints
  ([position size origin]
   {:position position :size size :origin origin})
  ([position origin]
   (layout-hints position nil origin))
  ([position]
   (layout-hints position nil (l/weighted-origin 0 0))))

(defn obtain-component-bbox [entity component]
  (let [bbox (:bbox entity)
        hints (-> component :layout-attributes :layout-hints)
        layout-ref (-> component :layout-attributes :layout-ref)
        {:keys [position size origin]} hints
        {:keys [left top]} position
        {:keys [width height]} size
        {:keys [orig-x orig-y]} origin]
    (if-let [layout (get-in entity [:layouts layout-ref])]
      (let [layout-pos  (l/absolute-position-of-layout entity layout)
            layout-size (l/absolute-size-of-layout entity layout)
            effective-width (cond
                               (= :wei (-> width :type)) (* (:width layout-size) (:value width))
                               (= :abs (-> width :type)) (:value width)
                               (= :exp (-> width :type)) ((provide (:value width)) entity layout)
                               :else (d/get-width component))
            effective-height (cond
                                (= :wei (-> height :type)) (* (:height layout-size) (:value height))
                                (= :abs (-> height :type)) (:value height)
                                (= :exp (-> height :type)) ((provide (:value height)) entity layout)
                                :else (d/get-height component))
            origin-offset-x (cond
                               (= :wei (-> orig-x :type)) (* effective-width (:value orig-x))
                               (= :abs (-> orig-x :type)) (:value orig-x)
                               (= :exp (-> orig-x :type)) ((provide (:value orig-x)) entity layout)
                               :else 0)
            origin-offset-y (cond
                               (= :wei (-> orig-y :type)) (* effective-height (:value orig-y))
                               (= :abs (-> orig-y :type)) (:value orig-y)
                               (= :exp (-> orig-y :type)) ((provide (:value orig-y)) entity layout)
                               :else 0)]
          {:width effective-width
           :height effective-height
           :left  (- (cond
                        (= :wei (-> left :type)) (+ (:left layout-pos) (* (:width layout-size) (:value left)))
                        (= :rel (-> left :type)) (+ (:left layout-pos ) (:value left))
                        (= :abs (-> left :type)) (:value left)
                        (= :exp (-> left :type)) ((provide (:value left)) entity layout bounds)
                        :else (d/get-left component)) origin-offset-x)
           :top  (- (cond
                        (= :wei (-> top :type)) (+ (:top layout-pos) (* (:height layout-size) (:value top)))
                        (= :rel (-> top :type)) (+ (:top layout-pos ) (:value top))
                        (= :abs (-> top :type)) (:value top)
                        (= :exp (-> top :type)) ((provide (:value top)) entity layout bounds)
                        :else (d/get-left component)) origin-offset-y)}))))

(defmethod l/create-context ::expression [entity layout] {})

(defmethod l/layout-function ::expression [entity component context]
  (when-let [bbox (obtain-component-bbox entity component)]
    (d/set-data component bbox))
  context)
