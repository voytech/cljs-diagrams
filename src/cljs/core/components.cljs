(ns core.components
  (:require [core.eventbus :as bus]))

(declare invoke-hook)

(defonce components (atom {}))

(defonce component-types (atom {}))

(defonce hooks (atom {}))

(defonce COMPONENT_CHANGED "component.changed")

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
  (contains-point? [this x y])
  (diff-properties [this other props])
  (diff-property [this p1 other p2]))

(defn- changed
  ([component properties]
   (bus/fire COMPONENT_CHANGED {:properties properties
                                :component component})))

(defrecord ComponentType [type rendering-method props init-data])

(defrecord Component [uid name type model rendering-method props]
  IDrawable
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
         (>= y (get-top this)) (<= y (+ (get-top this) (get-height this)))))
  (diff-properties [this other properties]
    (filterv #(not= (getp this %1) (getp other %2)) properties))
  (diff-property [this p1 other p2]
    (not= (getp this p1) (getp other p2))))

(defn- next-z-index []
  (or
    (let [vs (vals @components)]
      (when (and (not (nil? vs)) (< 0 (count vs)));
         (when-let [e (apply max-key #(getp % :z-index) vs)]
            (inc (getp e :z-index)))))
    1))

(defn- assert-z-index [component]
  (when (nil? (getp component :z-index))
    (setp component :z-index (next-z-index))))

(defn- add-component [component]
  (swap! components assoc (:uid component) component)
  (bus/fire "component.added" {:component component}))

(defn remove-component [component]
  (swap! components dissoc (:uid component))
  (bus/fire "component.removed" {:component component}))

(defn is-component [uid]
  (not (nil? (get @components uid))))

(defn define-component [type rendering-method props init-data]
 (swap! component-types assoc type (ComponentType. type rendering-method props init-data)))

(defn new-component
 ([type name data props method]
  (when-let [component-type (get @component-types type)]
    (let [_method (or method (:rendering-method component-type))
          _data  (merge (:init-data component-type) data)
          _props (merge (:props component-type) props)
          component (Component. (str (random-uuid)) name type (volatile! _data) _method _props)]
      (assert-z-index component)
      (bus/fire "component.created" {:component component})
      (add-component component)
      component)))
 ([type name data props]
  (new-component type name data props :default))
 ([type name data]
  (new-component type name data {} :default))
 ([type name]
  (new-component type name {} {} :default)))

(defn get-component-def [type]
 (get @components-types type))

(defn add-hook [type function hook]
  (swap! hooks assoc-in [type function] hook))

(defn- invoke-hook [drawable function & args]
  (let [type (:type drawable)]
     (when-let [hook (get-in @hooks [type function])]
        (apply hook drawable args))))
