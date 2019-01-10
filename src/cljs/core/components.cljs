(ns core.components
  (:require [core.eventbus :as bus]
            [core.state :as state]))


(defonce COMPONENT_CHANGED "component.changed")

(defprotocol IDrawable
  (update-state [this state])
  (state [this])
  (model [this])
  (setp [this property value])
  (silent-setp [this property value])
  (set-data [this map_])
  (silent-set-data [this map_])
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
  ([app-state component properties]
   (bus/fire app-state COMPONENT_CHANGED {:properties properties
                                          :component component})))

(defrecord LayoutAttributes [layout-ref layout-order layout-hints])

(defrecord Component [uid
                      name
                      type
                      model
                      rendering-method
                      attributes
                      parent-ref
                      layout-attributes
                      property-change-callback]
  IDrawable
  (model [this] @model)
  (setp [this property value]
    (vswap! model assoc property value)
    (property-change-callback this [property]))
  (silent-setp [this property value]
    (vswap! model assoc property value))
  (silent-set-data [this map_]
    (vswap! model merge map_))
  (set-data [this map_]
    (vswap! model merge map_)
    (property-change-callback this (keys map_)))
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

(defn resolve-z-index [val]
  (case val
    :top 100000
    :bottom 0
    val))

(defn- next-z-index [app-state]
  (or
    (let [components (or (state/get-in-diagram-state app-state [:components]) {})
          vs (filterv #(number? (getp % :z-index)) (vals components))]
      (when (and (not (nil? vs)) (< 0 (count vs)));
         (when-let [e (apply max-key #(getp % :z-index) vs)]
            (inc (getp e :z-index)))))
    1))

(defn- ensure-z-index [app-state component]
  (when (nil? (getp component :z-index))
    (setp component :z-index (next-z-index app-state))))

(defn remove-component [app-state component]
  (state/dissoc-diagram-state app-state [:components (:uid component)])
  (bus/fire app-state "component.removed" {:component component}))

(defn is-component [app-state uid]
  (not (nil? (state/get-in-diagram-state app-state [:components uid]))))

(defn components [app-state]
  (vals (state/get-in-diagram-state app-state [:components])))

(defn ordered-components [app-state]
  (sort-by #(getp % :z-index) > (components app-state)))

(defn default-model-callback [app-state bbox-draw]
  (fn [component properties]
     (if (and (some? bbox-draw)
              (some #(or (= :left %)
                         (= :top %)
                         (= :width %)
                         (= :height %)) properties))
       (let [alter-mdl (bbox-draw component)]
         (silent-set-data component alter-mdl)
         (changed app-state component (concat properties (keys alter-mdl))))
       (changed app-state component properties))))

(defn new-component
 ([app-state container arg-map]
   (let [{:keys [name
                 type
                 model
                 bbox-draw
                 attributes
                 layout-attributes
                 rendering-method
                 initializer
                 ]} arg-map
         initializer-data (if (nil? initializer) {} (initializer container attributes))
         template-data (-> container :components-properties type)
         mdl (merge initializer-data template-data model)
         callback (default-model-callback app-state bbox-draw)
         component (Component. (str (random-uuid))
                               name
                               type
                               (volatile! mdl)
                               rendering-method
                               attributes
                               (:uid container)
                               layout-attributes
                               callback)]
     (ensure-z-index app-state component)
     (bus/fire app-state "component.created" {:component component})
     (state/assoc-diagram-state app-state [:components (:uid component)] component)
     (bus/fire app-state "component.added" {:component component})
     (assoc-in container [:components (:name component)] component))))

(defn layout-attributes
  ([layout-ref layout-order layout-hints]
   (LayoutAttributes. layout-ref layout-order layout-hints))
  ([layout-ref layout-hints]
   (layout-attributes layout-ref 0 layout-hints))
  ([layout-ref]
   (layout-attributes layout-ref nil)))
