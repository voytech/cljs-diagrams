(ns impl.layouts.expression
  (:require [core.components :as d]
            [core.eventbus :as b]
            [core.entities :as e]
            [core.layouts :as l]))

(defrecord LayoutHints [position size origin])

(defn bound-expression [expr]
  {:type :exp :value expr})

(defn size-expression [width-exp height-exp]
  (l/LSize. {:type :exp :value width-exp}
            {:type :exp :value height-exp}))

(defn position-expression [left-exp top-exp]
  (l/LPosition. {:type :exp :value left-exp}
                {:type :exp :value top-exp}))

(defn width-of
  ([component-name margin]
    (fn [entity layout]
      (let [component (e/get-entity-component entity component-name)]
        (+ (d/get-width component) margin))))
  ([component-name]
    (width-of component-name 0)))

(defn height-of
  ([component-name margin]
    (fn [entity layout]
      (let [component (e/get-entity-component entity component-name)]
        (+ (d/get-height component) margin))))
  ([component-name]
    (height-of component-name 0)))

(defn left-of
  ([component-name margin]
    (fn [entity layout]
      (let [component (e/get-entity-component entity component-name)]
        (- (d/get-left component) margin))))
  ([component-name]
    (left-of component-name 0)))

(defn top-of
  ([component-name margin]
    (fn [entity layout]
      (let [component (e/get-entity-component entity component-name)]
        (- (d/get-top component) margin))))
  ([component-name]
    (top-of component-name 0)))

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
    (LayoutHints. position size origin))
  ([position origin]
    (LayoutHints. position nil origin))
  ([position]
    (LayoutHints. position nil (l/weighted-origin 0 0))))

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
                               (= :exp (-> width :type)) ((:value width) entity layout)
                               :else (d/get-width component))
            effective-height (cond
                                (= :wei (-> height :type)) (* (:height layout-size) (:value height))
                                (= :abs (-> height :type)) (:value height)
                                (= :exp (-> height :type)) ((:value height) entity layout)
                                :else (d/get-height component))
            origin-offset-x (cond
                               (= :wei (-> orig-x :type)) (* effective-width (:value orig-x))
                               (= :abs (-> orig-x :type)) (:value orig-x)
                               (= :exp (-> orig-x :type)) ((:value orig-x) entity layout)
                               :else 0)
            origin-offset-y (cond
                               (= :wei (-> orig-y :type)) (* effective-height (:value orig-y))
                               (= :abs (-> orig-y :type)) (:value orig-y)
                               (= :exp (-> orig-y :type)) ((:value orig-y) entity layout)
                               :else 0)]
          {:width effective-width
           :height effective-height
           :left  (- (cond
                        (= :wei (-> left :type)) (+ (:left layout-pos) (* (:width layout-size) (:value left)))
                        (= :rel (-> left :type)) (+ (:left layout-pos ) (:value left))
                        (= :abs (-> left :type)) (:value left)
                        (= :exp (-> left :type)) ((:value left) entity layout bounds)
                        :else (d/get-left component)) origin-offset-x)
           :top  (- (cond
                        (= :wei (-> top :type)) (+ (:top layout-pos) (* (:height layout-size) (:value top)))
                        (= :rel (-> top :type)) (+ (:top layout-pos ) (:value top))
                        (= :abs (-> top :type)) (:value top)
                        (= :exp (-> top :type)) ((:value top) entity layout bounds)
                        :else (d/get-left component)) origin-offset-y)}))))

(defmethod l/create-context ::expression [layout] {})

(defn expression-layout [entity component context]
  (when-let [bbox (obtain-component-bbox entity component)]
    (d/set-data component bbox))
  context)
