(ns core.drawables
  (:require [core.eventbus :as bus]))

(defonce watchers (atom {}))

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


(defn- register-watcher [drawable property]
  (when (nil? (get @watchers [(:uid drawable) property]))
    (let [model (:model drawable)]
      (add-watch model property (fn [key atom old-state new-state]
                                  (bus/fire DRAWABLE_CHANGED {:property key
                                                              :old (property old-state)
                                                              :new (property new-state)
                                                              :drawable drawable})))
      (swap! watchers assoc-in [(:uid drawable) property] true))))

(defrecord Drawable [uid type model rendering-state]
  IDrawable
  (update-state [this state] (reset! rendering-state state))
  (state [this] @rendering-state)
  (model [this] @model)
  (setp [this property value]
    (register-watcher this property)
    (swap! model assoc property value))
  (set-data [this map_]
    (doseq [key (keys map_)]
      (setp this key (get map_ key))))
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
          obbox (get-bbox other)
          test (fn [tbbox obbox]
                 (and (<= (:left tbbox) (:left obbox)) (>= (+ (:left tbbox) (:width tbbox)) (:left obbox))
                      (<= (:top tbbox) (:top obbox)) (>= (+ (:top tbbox) (:height tbbox)) (:top obbox))))]
      (or (test tbbox obbox)
          (test obbox tbbox))))

  (contains? [this other])
  (contains-point? [this x y]
    (and (>= x (get-left this)) (<= x (+ (get-left this) (get-width this)))
         (>= y (get-top this)) (<= y (+ (get-top this) (get-height this))))))


(defn create-drawable
  ([type]
   (create-drawable type {}))
  ([type data]
   (let [drawable (Drawable. (str (random-uuid)) type (atom {}) (atom {}) nil)]
     (set-data drawable data)
     drawable)))
