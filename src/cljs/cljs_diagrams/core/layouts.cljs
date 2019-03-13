(ns cljs-diagrams.core.layouts
  (:require [cljs-diagrams.core.components :as d]
            [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.entities :as e]
            [cljs-diagrams.core.eventbus :as b]))

(spec/def ::coord-value (spec/keys :req-un [::value
                                            ::type]))

(spec/def ::layout-position (spec/keys :req-un [::left
                                                ::top]))

(spec/def ::layout-size (spec/keys :req-un [::width
                                            ::height]))

(spec/def ::layout-origin (spec/keys :req-un [::orig-x
                                              ::orig-y]))

(spec/def ::layout-margins (spec/keys :req-un [::margin-left
                                               ::margin-top
                                               ::margin-bottom
                                               ::margin-right]))

(spec/def ::layout  (spec/keys :req-un [::name
                                        ::type
                                        ::position
                                        ::size
                                        ::margins]))

(defn position [left top]
  {:pre [spec/valid? ::coord-value left]}
  {:pre [spec/valid? ::coord-value top]}
  {:left left :top top})

(defn size [width height]
  {:pre [spec/valid? ::coord-value width]}
  {:pre [spec/valid? ::coord-value height]}
  {:width width :height height})

(defn origin [orig-x orig-y]
  {:pre [spec/valid? ::coord-value orig-x]}
  {:pre [spec/valid? ::coord-value orig-y]}
  {:orig-x orig-x :orig-y orig-y})

(defn margins [margin-left margin-top margin-bottom margin-right]
  {:margin-left margin-left
   :margin-top margin-top
   :margin-bottom margin-bottom
   :margin-right margin-right})

(defn size-of-type [width height type]
  (size {:value width :type type}
        {:value height :type type}))

(defn position-of-type [left top type]
  (position {:value left :type type}
            {:value top :type type}))

(defn origin-of-type [orig-x orig-y type]
  (origin  {:value orig-x :type type}
           {:value orig-y :type type}))

(defn weighted-size [width height]
  (size-of-type width height :wei))

(defn weighted-position [left top]
  (position-of-type left top :wei))

(defn weighted-origin [orig-x orig-y]
  (origin-of-type orig-x orig-y :wei))

(defn relative-position [left top]
  (position-of-type left top :rel))

(defn absolute-position [left top]
  (position-of-type left top :abs))

(defn match-parent-size []
  (weighted-size 1 1))

(defn match-parent-position []
  (weighted-position 0 0))

(defn layout
  ([name type position size margins]
   {:name name :type type :position position :size size :margins margins})
  ([name type position margins]
   (layout name type position (match-parent-size) margins))
  ([name type position]
   (layout name type position (match-parent-size) nil))
  ([name type]
   (layout name type (match-parent-position) (match-parent-size) nil))
  ([data]
   (let [{:keys [name type position size margins]} data]
     (layout name type
             (position (:left position) (:top position))
             (size (:width size) (:height size))
             (margins (:margin-left margins)
                      (:margin-top margins)
                      (:margin-bottom margins)
                      (:margin-right margins))))))

(defn absolute-position-of-layout [entity layout]
  (let [bbox (:bbox entity)]
    (let [pos (:position layout)
          left (-> pos :left :value)
          top  (-> pos :top :value)]
      {:left (cond
               (= :wei (-> pos :left :type)) (+ (:left bbox) (* (:width bbox) left))
               (= :rel (-> pos :left :type)) (+ (:left bbox) left)
               :else left)
       :top  (cond
                (= :wei (-> pos :top :type)) (+ (:top bbox) (* (:height bbox) top))
                (= :rel (-> pos :top :type)) (+ (:top bbox) top)
                :else top)})))

(defn absolute-size-of-layout [entity layout]
  (let [bbox (:bbox entity)]
    (let [size (:size layout)
          width (-> size :width :value)
          height (-> size :height :value)]
      {:width (cond
                (= :wei (-> size :width :type)) (* (:width bbox) width)
                :else width)
       :height (cond
                 (= :wei (-> size :height :type)) (* (:height bbox) height)
                 :else height)})))

(defn move-entity-to [app-state entity new-pos]
  (let [old-bbox (:bbox entity)
        new-bbox (merge old-bbox new-pos)]
     (do-layouts entity)))

(defn move-entity-by [app-state entity offset])

(defmulti create-context (fn [entity layout] (:type layout)))

(defmulti layout-function (fn [entity component context] (:layout-type context)))

(defn create-evaluation-context [entity]
   (->> (mapv (fn [layout]
                 {(:name layout) (assoc (create-context entity layout) :layout-type (:type layout))})
              (-> entity :layouts vals))
        (apply merge)))

(defn do-layouts [entity]
  (let [context (volatile! (create-evaluation-context entity))]
    (doseq [component (->> (e/components-of entity)
                           (filterv #(some? (-> % :layout-attributes :layout-ref)))
                           (sort-by #(-> % :layout-attributes :layout-order)))]
      (when-let [layout (get-in entity [:layouts (-> component :layout-attributes :layout-ref)])]
        (vswap! context assoc (:name layout)
          (layout-function entity
                           component
                           (get @context (:name layout))))))))

(defn initialize [app-state]
  (b/on app-state ["layouts.do"] -999 (fn [event]
                                        (when-let [{:keys [container app-state]} (:context event)]
                                          (do-layouts (e/entity-by-id app-state (:uid container)))))))
