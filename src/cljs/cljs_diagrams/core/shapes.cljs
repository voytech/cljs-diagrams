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
   (-> (r/mark-for-redraw app-state shape properties)
       (bus/fire SHAPE_CHANGED {:properties properties
                                :shape shape}))))

(defp bbox-predicate []
  (fn [properties]
    (some #(or (= :left %)
               (= :top %)
               (= :width %)
               (= :height %)) properties)))

(defn bbox-draw [provider]
  {:pred (bbox-predicate) :func (provider)})

(defn customize-model [customizers]
  (fn [app-state shape properties]
    (let [agg-properties (volatile! properties)
          intermediate-state (volatile! app-state)]
      (doseq [customizer customizers]
        (let [predicate (provide (:pred customizer))
              func (provide (:func customizer))]
          (when (predicate properties)
            (let [{:keys [new-state target]} (func @intermediate-state shape)]
              (vreset! intermediate-state (silent-set-data new-state shape target))
              (vswap! agg-properties concat (keys target))))))
      (changed @intermediate-state shape @agg-properties))))

(defn model [shape] (:model shape))

(defn silent-setp [app-state shape property value]
  (state/assoc-diagram-state app-state [:nodes (:parent-ref shape)
                                        :shapes (:name shape)
                                        :model property]
                                       value))

(defn setp [app-state shape property value]
  (let [new-app-state (silent-setp app-state shape property value)
        listener (:model-listener shape)]
    (listener app-state shape [property])))

(defn silent-set-data [app-state shape map_]
  (let [model (state/get-in-diagram-state app-state [:nodes (:parent-ref shape) :shapes (:name shape) :model])
        merged (merge model map_)]
  (state/assoc-diagram-state app-state [:nodes (:parent-ref shape) :shapes (:name shape) :model] merged)))

(defn set-data [app-state shape map_]
  (let [new-state (silent-set-data app-state shape map_)
        listener (:model-listener shape)]
    (listener app-state shape (keys map_))))

(defn getp [app-state shape property]
  (state/get-in-diagram-state app-state [:nodes (:parent-ref shape) :shapes (:name shape) :model property]))

(defn set-border-color [app-state shape value]
  (setp app-state shape :border-color value))

(defn set-background-color [app-state shape value]
  (setp app-state shape :background-color value))

(defn set-border-style [app-state shape value]
  (setp app-state shape :border-style value))

(defn set-border-width [app-state shape value]
  (setp app-state shape :border-width value))

(defn set-left [app-state shape value]
  (setp app-state shape :left value))

(defn set-top [app-state shape value]
  (setp app-state shape :top value))

(defn set-width [app-state shape value]
  (setp app-state shape :width value))

(defn set-height [app-state shape value]
  (setp app-state shape :height value))

(defn get-left [app-state shape]
  (getp app-state shape :left))

(defn get-top [app-state shape]
  (getp app-state shape :top))

(defn get-width [app-state shape]
  (getp app-state shape :width))

(defn get-height [app-state shape]
  (getp app-state shape :height))

(defn get-bbox [app-state shape]
  {:left (get-left app-state shape)
   :top (get-top app-state shape)
   :width (get-width app-state shape)
   :height (get-height app-state shape)})

(defn intersects? [app-state shape other]
  (let [tbbox (get-bbox app-state shape)
        obbox (get-bbox app-state other)]
    (or
     (and (<= (:left tbbox) (:left obbox)) (>= (+ (:left tbbox) (:width tbbox)) (:left obbox))
          (<= (:top tbbox) (:top obbox)) (>= (+ (:top tbbox) (:height tbbox)) (:top obbox)))
     (and (<= (:left tbbox) (:left obbox)) (>= (+ (:left tbbox) (:width tbbox)) (:left obbox))
          (<= (:top obbox) (:top tbbox)) (>= (+ (:top obbox) (:height obbox)) (:top tbbox)))
     (and (<= (:left obbox) (:left tbbox)) (>= (+ (:left obbox) (:width obbox)) (:left tbbox))
          (<= (:top obbox) (:top tbbox)) (>= (+ (:top obbox) (:height obbox)) (:top tbbox)))
     (and (<= (:left obbox) (:left tbbox)) (>= (+ (:left obbox) (:width obbox)) (:left tbbox))
          (<= (:top tbbox) (:top obbox)) (>= (+ (:top tbbox) (:height tbbox)) (:top obbox))))))

(defn contains? [app-state shape other])

(defn contains-point? [app-state shape x y]
  (and (>= x (get-left app-state shape)) (<= x (+ (get-left app-state shape) (get-width app-state shape)))
       (>= y (get-top app-state shape)) (<= y (+ (get-top app-state shape) (get-height app-state shape)))))

(defn diff-properties [app-state shape other properties]
  (filterv #(not= (getp app-state shape %1) (getp app-state other %2)) properties))

(defn diff-property [app-state shape p1 other p2]
  (not= (getp app-state shape p1) (getp app-state other p2)))

(defn resolve-z-index [val]
  (case val
    :top 100000
    :before-top 99999
    :before-bottom 1
    :bottom 0
    val))

(defn get-all-shapes [app-state]
  (->> (or (state/get-in-diagram-state app-state [:shapes]) {})
       vals
       (mapv #(state/get-in-diagram-state app-state [:nodes (:parent-ref %) :shapes (:name %)]))))

(defn- next-z-index [app-state]
  (or
    (let [shapes (get-all-shapes app-state)
          vs (filterv #(number? (getp app-state % :z-index)) shapes)]
      (when (and (not (nil? vs)) (< 0 (count vs)));
         (when-let [e (apply max-key #(getp app-state % :z-index) vs)]
            (inc (getp app-state e :z-index)))))
    1))

(defn- ensure-z-index [app-state shape]
  (if (nil? (getp app-state shape :z-index))
    (setp app-state shape :z-index (next-z-index app-state))
     app-state))

(defn remove-shape [app-state shape]
  (-> app-state
      (state/dissoc-diagram-state [:shapes (:uid shape)])
      (r/remove-shape-rendering-state shape)
      (bus/fire "component.removed" {:shape shape})))

(defn is-shape [app-state uid]
  (not (nil? (state/get-in-diagram-state app-state [:shapes uid]))))

(defn shapes [app-state]
  (vals (state/get-in-diagram-state app-state [:shapes])))

(defn ordered-shapes [app-state]
  (sort-by #(getp % :z-index) > (shapes app-state)))

(defn new-shape [app-state container arg-map]
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
        listener (customize-model model-customizers)
        shape {:uid (str (random-uuid))
               :name name
               :type type
               :model mdl
               :rendering-method rendering-method
               :attributes attributes
               :parent-ref (:uid container)
               :layout-attributes layout-attributes
               :model-listener listener
               :model-customizers model-customizers}]
    {:target shape
     :new-state (-> app-state
                    (state/assoc-diagram-state [:nodes (:parent-ref shape) :shapes name] shape)
                    (state/assoc-diagram-state [:shapes (:uid shape)] (select-keys [:parent-ref :name]))
                    (ensure-z-index shape)
                    (bus/fire "shape.created" {:shape shape})
                    (r/mark-all-for-redraw shape)
                    (bus/fire "shape.added" {:shape shape}))}))

(defn layout-attributes
  ([layout-ref layout-order layout-hints]
   {:pre [spec/valid? ::layout-attributes {:layout-ref layout-ref :layout-order layout-order :layout-hints layout-hints}]}
   {:layout-ref layout-ref :layout-order layout-order :layout-hints layout-hints})
  ([layout-ref layout-hints]
   (layout-attributes layout-ref 0 layout-hints))
  ([layout-ref]
   (layout-attributes layout-ref nil)))
