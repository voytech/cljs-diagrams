(ns impl.layouts.weighted
  (:require [core.components :as d]
            [core.eventbus :as b]
            [core.layouts :as l]))

(defrecord LayoutHints [position size origin])

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
                               :else (d/get-width component))
            effective-height (cond
                                (= :wei (-> height :type)) (* (:height layout-size) (:value height))
                                (= :abs (-> height :type)) (:value height)
                                :else (d/get-height component))
            origin-offset-x (cond
                               (= :wei (-> orig-x :type)) (* effective-width (:value orig-x))
                               (= :abs (-> orig-x :type)) (:value orig-x)
                               :else 0)
            origin-offset-y (cond
                               (= :wei (-> orig-y :type)) (* effective-height (:value orig-y))
                               (= :abs (-> orig-y :type)) (:value orig-y)
                               :else 0)]
          {:width effective-width
           :height effective-height
           :left  (- (cond
                        (= :wei (-> left :type)) (+ (:left layout-pos) (* (:width layout-size) (:value left)))
                        (= :rel (-> left :type)) (+ (:left layout-pos ) (:value left))
                        (= :abs (-> left :type)) (:value left)
                        :else (d/get-left component)) origin-offset-x)
           :top  (- (cond
                        (= :wei (-> top :type)) (+ (:top layout-pos) (* (:height layout-size) (:value top)))
                        (= :rel (-> top :type)) (+ (:top layout-pos ) (:value top))
                        (= :abs (-> top :type)) (:value top)
                        :else (d/get-left component)) origin-offset-y)}))))

(defmethod l/create-context ::weighted [layout] {})

(defn weighted-layout [entity component context]
  (when-let [bbox (obtain-component-bbox entity component)]
    (d/set-data component bbox))
  context)
