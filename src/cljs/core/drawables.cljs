(ns core.drawables
  (:require [core.eventbus :as bus]))

(declare invoke-hook)

(defonce drawables (atom {}))

(defonce hooks (atom {}))

(defonce standard-properties [:left :top :width :height :border-color :border-width
                              :border-style :color :background-color :opacity
                              :fill :fill-type :angle :z-index])

(defonce standard-property-values {:border-style [:dotted :dashed :solid]
                                   :fill-type [:border-only :full :gradient :no-fill]})

(defonce DRAWABLE_CHANGED "drawable.changed")

(defprotocol IDrawable
  (update-state [this state])
  (state [this])
  (model [this])
  (setp [this property value])
  (set-data [this map_])
  (getp [this property])
  (set-border-color [this color])
  (set-background-color [this color])
  (set-border-style [this style])
  (set-border-width [this width])
  (set-left [this left])
  (set-top [this top])
  (set-width [this width])
  (set-height [this height])
  (get-left [this])
  (get-top [this])
  (get-width [this])
  (get-height [this])
  (get-bbox [this])
  (intersects? [this other])
  (contains? [this other])
  (contains-point? [this x y]))

(defn- changed
  ([drawable properties]
   (bus/fire DRAWABLE_CHANGED {:properties properties
                               :drawable drawable})))

(defrecord Drawable [uid type model rendering-state]
  IDrawable
  (update-state [this state] (vreset! rendering-state state))
  (state [this] @rendering-state)
  (model [this] @model)
  (setp [this property value]
    (vswap! model assoc property value)
    (changed this [property])
    (invoke-hook this :setp property value))
  (set-data [this map_]
    (vswap! model merge map_)
    (changed this (keys map_))
    (invoke-hook this :set-data map_))
  (getp [this property] (get @model property))
  (set-border-color [this value] (setp this :border-color value))
  (set-background-color [this value] (setp this :background-color value))
  (set-border-style [this value] (setp this :border-style value))
  (set-border-width [this value] (setp this :border-width value))
  (set-left [this value] (setp this :left value))
  (set-top [this value] (setp this :top value))
  (set-width [this value] (setp this :width value))
  (set-height [this value] (setp this :height value))
  (get-left [this] (getp this :left))
  (get-top [this] (getp this :top))
  (get-width [this] (getp this :width))
  (get-height [this] (getp this :height))
  (get-bbox [this] {:left (get-left this)
                    :top (get-top this)
                    :width (get-width this)
                    :height (get-height this)})
  (intersects? [this other]
    (let [tbbox (get-bbox this)
          obbox (get-bbox other)]
      (or
       (and (<= (:left tbbox) (:left obbox)) (>= (+ (:left tbbox) (:width tbbox)) (:left obbox))
            (<= (:top tbbox) (:top obbox)) (>= (+ (:top tbbox) (:height tbbox)) (:top obbox)))
       (and (<= (:left tbbox) (:left obbox)) (>= (+ (:left tbbox) (:width tbbox)) (:left obbox))
            (<= (:top obbox) (:top tbbox)) (>= (+ (:top obbox) (:height obbox)) (:top tbbox)))
       (and (<= (:left obbox) (:left tbbox)) (>= (+ (:left obbox) (:width obbox)) (:left tbbox))
            (<= (:top obbox) (:top tbbox)) (>= (+ (:top obbox) (:height obbox)) (:top tbbox)))
       (and (<= (:left obbox) (:left tbbox)) (>= (+ (:left obbox) (:width obbox)) (:left tbbox))
            (<= (:top tbbox) (:top obbox)) (>= (+ (:top tbbox) (:height tbbox)) (:top obbox))))))

  (contains? [this other])
  (contains-point? [this x y]
    (and (>= x (get-left this)) (<= x (+ (get-left this) (get-width this)))
         (>= y (get-top this)) (<= y (+ (get-top this) (get-height this))))))

(defn- next-z-index []
  (or
    (let [vs (vals @drawables)]
      (when (and (not (nil? vs)) (< 0 (count vs)));
         (when-let [e (apply max-key #(getp % :z-index) vs)]
            (inc (getp e :z-index)))))
    1))

(defn- assert-z-index [drawable]
  (when (nil? (getp drawable :z-index))
    (setp drawable :z-index (next-z-index))))

(defn- add-drawable [drawable]
  (swap! drawables assoc (:uid drawable) drawable))

(defn remove-drawable [drawable]
  (swap! drawables dissoc (:uid drawable))
  (bus/fire "drawable.removed" {:drawable drawable}))

(defn is-drawable [uid]
  (not (nil? (get @drawables uid))))

(defn create-drawable
  ([type]
   (create-drawable type {}))
  ([type data]
   (let [drawable (Drawable. (str (random-uuid)) type (volatile! {}) (volatile! {}) nil)]
     (set-data drawable data)
     (assert-z-index drawable)
     (add-drawable drawable)
     (bus/fire "drawable.created" {:drawable drawable})
     drawable)))

(defn add-hook [type function hook]
  (swap! hooks assoc-in [type function] hook))

(defn- invoke-hook [drawable function & args]
  (let [type (:type drawable)]
     (when-let [hook (get-in @hooks [type function])]
        (apply hook drawable args))))
