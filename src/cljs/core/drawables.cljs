(ns core.drawables
  (:require [core.eventbus :as bus]))

(defonce watchers (atom {}))

(defonce DRAWABLE_CHANGED "drawable.changed")

(defprotocol IDrawable
  (update-state [this state])
  (set [this property value])
  (get [this property])
  (set-border-color [this color])
  (set-background-Color [this color])
  (set-border-style [this style])
  (set-border-width [this width])
  (set-left [this left])
  (set-top [this top])
  (set-width [this width])
  (set-height [this height])
  (get-left [this])
  (get-top [this])
  (get-width [this])
  (get-geight [this])
  (get-bbox [this])
  (intersects? [this other])
  (contains? [this other]))

(defn- register-watcher [drawable property]
  (when (nil? (get @watchers [(:uid drawable) property]))
    (let [model (:model drawable)]
      (add-watch model property (fn [key atom old-state new-state]
                                  (bus/fire DRAWABLE_CHANGED {:property key
                                                              :old old-state
                                                              :new new-state
                                                              :drawable drawable})))
      (swap! watchers assoc-in [(:uid drawable) property] true))))

(defrecord Drawable [uid type model rendering-state]
  IDrawable
  (update-state [this state] (swap! this assoc :rendering-state state))
  (set [this property value]
    (register-watcher this property)
    (swap! model assoc property value))
  (get [this property value] (get @model property))
  (set-border-color [this value] (set this :border-color value))
  (set-border-style [this value] (set this :border-style value))
  (set-border-width [this value] (set this :border-width value))
  (set-left [this value] (set this :left value))
  (set-top [this value] (set this :top value))
  (set-width [this value] (set this :width value))
  (set-height [this value] (set this :height value))
  (get-left [this] (get this :left))
  (get-top [this] (get this :top))
  (get-width [this] (get this :width))
  (get-height [this] (get this :height))
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

  (contains? [this other]))
