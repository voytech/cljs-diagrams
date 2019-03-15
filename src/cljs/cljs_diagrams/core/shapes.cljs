(ns cljs-diagrams.core.shapes
  (:require [cljs-diagrams.core.eventbus :as bus]
            [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.funcreg :refer [provide]]
            [cljs-diagrams.core.state :as state]
            [cljs-diagrams.core.rendering :as r])
  (:require-macros [cljs-diagrams.core.macros :refer [defp]]))

(spec/def ::shape (spec/keys :req-un [::uid
                                      ::name
                                      ::type
                                      ::model
                                      ::rendering-method
                                      ::attributes
                                      ::layout-attributes
                                      ::model-listener
                                      ::model-customizers
                                      ::parent-ref]))

(spec/def ::layout-attributes (spec/keys :req-un [::layout-ref
                                                  ::layout-order
                                                  ::layout-hints]))

(defonce SHAPE_CHANGED "shape.changed")

(defn- changed
  ([app-state shape properties]
   (r/mark-for-redraw app-state shape properties)
   (bus/fire app-state SHAPE_CHANGED {:properties properties
                                      :shape shape})))

(defp bbox-predicate []
  (fn [properties]
    (some #(or (= :left %)
               (= :top %)
               (= :width %)
               (= :height %)) properties)))

(defn bbox-draw [provider]
  {:pred (bbox-predicate) :func (provider)})

(defn customize-model [app-state customizers]
  (fn [shape properties]
    (let [agg-properties (volatile! properties)]
      (doseq [customizer customizers]
        (let [predicate (provide (:pred customizer))
              func (provide (:func customizer))]
          (when (predicate properties)
            (let [model-increment (func shape)]
              (silent-set-data shape model-increment)
              (vswap! agg-properties concat (keys model-increment))))))
      (changed app-state shape @agg-properties))))

(defn model [shape] (-> shape :model deref))

(defn setp [shape property value]
  (vswap! (:model shape) assoc property value)
  (let [listener (:model-listener shape)]
    (listener shape [property])))

(defn silent-setp [shape property value]
  (vswap! (:model shape) assoc property value))

(defn silent-set-data [shape map_]
  (vswap! (:model shape) merge map_))

(defn set-data [shape map_]
  (vswap! (:model shape) merge map_)
  (let [listener (:model-listener shape)]
    (listener shape (keys map_))))

(defn getp [shape property]
  (-> shape :model deref property))

(defn set-border-color [shape value]
  (setp shape :border-color value))

(defn set-background-color [shape value]
  (setp shape :background-color value))

(defn set-border-style [shape value]
  (setp shape :border-style value))

(defn set-border-width [shape value]
  (setp shape :border-width value))

(defn set-left [shape value]
  (setp shape :left value))

(defn set-top [shape value]
  (setp shape :top value))

(defn set-width [shape value]
  (setp shape :width value))

(defn set-height [shape value]
  (setp shape :height value))

(defn get-left [shape]
  (getp shape :left))

(defn get-top [shape]
  (getp shape :top))

(defn get-width [shape]
  (getp shape :width))

(defn get-height [shape]
  (getp shape :height))

(defn get-bbox [shape]
  {:left (get-left shape)
   :top (get-top shape)
   :width (get-width shape)
   :height (get-height shape)})

(defn intersects? [shape other]
  (let [tbbox (get-bbox shape)
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

(defn contains? [shape other])

(defn contains-point? [shape x y]
  (and (>= x (get-left shape)) (<= x (+ (get-left shape) (get-width shape)))
       (>= y (get-top shape)) (<= y (+ (get-top shape) (get-height shape)))))

(defn diff-properties [shape other properties]
  (filterv #(not= (getp shape %1) (getp other %2)) properties))

(defn diff-property [shape p1 other p2]
  (not= (getp shape p1) (getp other p2)))

(defn resolve-z-index [val]
  (case val
    :top 100000
    :before-top 99999
    :before-bottom 1
    :bottom 0
    val))

(defn- next-z-index [app-state]
  (or
    (let [shapes (or (state/get-in-diagram-state app-state [:shapes]) {})
          vs (filterv #(number? (getp % :z-index)) (vals shapes))]
      (when (and (not (nil? vs)) (< 0 (count vs)));
         (when-let [e (apply max-key #(getp % :z-index) vs)]
            (inc (getp e :z-index)))))
    1))

(defn- ensure-z-index [app-state shape]
  (when (nil? (getp shape :z-index))
    (setp shape :z-index (next-z-index app-state))))

(defn remove-shape [app-state shape]
  (state/dissoc-diagram-state app-state [:shapes (:uid shape)])
  (r/remove-shape app-state shape)
  (bus/fire app-state "component.removed" {:shape shape}))

(defn is-shape [app-state uid]
  (not (nil? (state/get-in-diagram-state app-state [:shapes uid]))))

(defn shapes [app-state]
  (vals (state/get-in-diagram-state app-state [:shapes])))

(defn ordered-shapes [app-state]
  (sort-by #(getp % :z-index) > (shapes app-state)))

(defn new-shape
 ([app-state container arg-map]
  (let [{:keys [name
                type
                model
                model-customizers
                attributes
                layout-attributes
                rendering-method
                initializer]}
        arg-map
        initializer-data (if (nil? initializer) {} (initializer container attributes))
        template-data (get-in container [:shapes-properties name])
        mdl (merge initializer-data template-data model)
        listener (customize-model app-state model-customizers)
        shape {:uid (str (random-uuid))
               :name name
               :type type
               :model (volatile! mdl)
               :rendering-method rendering-method
               :attributes attributes
               :parent-ref (:uid container)
               :layout-attributes layout-attributes
               :model-listener listener
               :model-customizers model-customizers}]
    (ensure-z-index app-state shape)
    (bus/fire app-state "component.created" {:shape shape})
    (state/assoc-diagram-state app-state [:shapes (:uid shape)] shape)
    (r/mark-all-for-redraw app-state shape)
    (bus/fire app-state "component.added" {:shape shape})
    (assoc-in container [:shapes (:name shape)] shape))))

(defn layout-attributes
  ([layout-ref layout-order layout-hints]
   {:pre [spec/valid? ::layout-attributes {:layout-ref layout-ref :layout-order layout-order :layout-hints layout-hints}]}
   {:layout-ref layout-ref :layout-order layout-order :layout-hints layout-hints})
  ([layout-ref layout-hints]
   (layout-attributes layout-ref 0 layout-hints))
  ([layout-ref]
   (layout-attributes layout-ref nil)))
