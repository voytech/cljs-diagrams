(ns cljs-diagrams.core.components
  (:require [cljs-diagrams.core.eventbus :as bus]
            [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.state :as state]))

(spec/def ::component (spec/keys :req-un [::uid
                                          ::name
                                          ::type
                                          ::model
                                          ::rendering-method
                                          ::attributes
                                          ::layout-attributes
                                          ::property-change-callback
                                          ::parent-ref]))

(spec/def ::layout-attributes (spec/keys :req-un [::layout-ref
                                                  ::layout-order
                                                  ::layout-hints]))

(defonce COMPONENT_CHANGED "component.changed")

(defn- changed
  ([app-state component properties]
   (bus/fire app-state COMPONENT_CHANGED {:properties properties
                                          :component component})))

(defn model [component] (-> component :model deref))

(defn setp [component property value]
  (vswap! (:model component) assoc property value)
  (let [pcc (:property-change-callback component)]
    (pcc component [property])))

(defn silent-setp [component property value]
  (vswap! (:model component) assoc property value))

(defn silent-set-data [component map_]
  (vswap! (:model component) merge map_))

(defn set-data [component map_]
  (vswap! (:model component) merge map_)
  (let [pcc (:property-change-callback component)]
    (pcc component (keys map_))))

(defn getp [component property]
  (-> component :model deref property))

(defn set-border-color [component value]
  (setp component :border-color value))

(defn set-background-color [component value]
  (setp component :background-color value))

(defn set-border-style [component value]
  (setp component :border-style value))

(defn set-border-width [component value]
  (setp component :border-width value))

(defn set-left [component value]
  (setp component :left value))

(defn set-top [component value]
  (setp component :top value))

(defn set-width [component value]
  (setp component :width value))

(defn set-height [component value]
  (setp component :height value))

(defn get-left [component]
  (getp component :left))

(defn get-top [component]
  (getp component :top))

(defn get-width [component]
  (getp component :width))

(defn get-height [component]
  (getp component :height))

(defn get-bbox [component]
  {:left (get-left component)
   :top (get-top component)
   :width (get-width component)
   :height (get-height component)})

(defn intersects? [component other]
  (let [tbbox (get-bbox component)
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

(defn contains? [component other])

(defn contains-point? [component x y]
  (and (>= x (get-left component)) (<= x (+ (get-left component) (get-width component)))
       (>= y (get-top component)) (<= y (+ (get-top component) (get-height component)))))

(defn diff-properties [component other properties]
  (filterv #(not= (getp component %1) (getp other %2)) properties))

(defn diff-property [component p1 other p2]
  (not= (getp component p1) (getp other p2)))

(defn resolve-z-index [val]
  (case val
    :top 100000
    :before-top 99999
    :before-bottom 1
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
                initializer]}
        arg-map
        initializer-data (if (nil? initializer) {} (initializer container attributes))
        template-data (get-in container [:components-properties name])
        mdl (merge initializer-data template-data model)
        callback (default-model-callback app-state bbox-draw)
        component {:uid (str (random-uuid))
                   :name name
                   :type type
                   :model (volatile! mdl)
                   :rendering-method rendering-method
                   :attributes attributes
                   :parent-ref (:uid container)
                   :layout-attributes layout-attributes
                   :property-change-callback callback}]
    (ensure-z-index app-state component)
    (bus/fire app-state "component.created" {:component component})
    (state/assoc-diagram-state app-state [:components (:uid component)] component)
    (bus/fire app-state "component.added" {:component component})
    (assoc-in container [:components (:name component)] component))))

(defn layout-attributes
  ([layout-ref layout-order layout-hints]
   {:pre [spec/valid? ::layout-attributes {:layout-ref layout-ref :layout-order layout-order :layout-hints layout-hints}]}
   {:layout-ref layout-ref :layout-order layout-order :layout-hints layout-hints})
  ([layout-ref layout-hints]
   (layout-attributes layout-ref 0 layout-hints))
  ([layout-ref]
   (layout-attributes layout-ref nil)))
