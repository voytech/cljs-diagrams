(ns core.layouts
  (:require [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]))

(defrecord LPosition [left top])

(defrecord LSize [width height])

(defrecord LOrigin [orig-x orig-y])

(defrecord Margins [margin-left margin-top margin-bottom margin-right])

(defrecord Layout [name layout-func position size margins])

(defrecord LayoutEvaluationContext [entity context])

(defn margins [margin-left margin-top margin-bottom margin-right]
  (Margins. margin-left margin-top margin-bottom margin-right))

(defn size-of-type [width height type]
  (LSize. {:value width :type type}
          {:value height :type type}))

(defn position-of-type [left top type]
  (LPosition. {:value left :type type}
              {:value top :type type}))

(defn origin-of-type [orig-x orig-y type]
  (LOrigin. {:value orig-x :type type}
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

(defn origin [orig-x orig-y]
  (origin-of-type orig-x orig-y :abs))

(defn size
  ([width height]
    (size-of-type width height :abs))
  ([type-width width type-height height]
    (LSize. {:value width :type type-width}
            {:value height :type type-height})))

(defn match-parent-size []
  (weighted-size 1 1))

(defn match-parent-position []
  (weighted-position 0 0))

(defn layout
  ([name layout-func position size margins]
    (Layout. name layout-func position size margins))
  ([name layout-func position margins]
    (Layout. name layout-func position (match-parent-size) margins))
  ([name layout-func position]
    (Layout. name layout-func position (match-parent-size) nil))
  ([name layout-func]
    (Layout. name layout-func (match-parent-position) (match-parent-size) nil)))

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

(defmulti create-context :name)

(defn create-evaluation-context [entity]
   (->> (mapv (fn [layout]
                 {(:name layout) (create-context layout)})
               (-> entity :layouts vals))
        (apply merge)))

(defn do-layouts [entity]
  (let [context (volatile! (create-evaluation-context entity))]
    (doseq [component (->> (e/components-of entity)
                           (filterv #(some? (-> % :layout-attributes :layout-ref)))
                           (sort-by #(-> % :layout-attributes :layout-order)))]
      (when-let [layout (get-in entity [:layouts (-> component :layout-attributes :layout-ref)])]
        (vswap! context assoc (:name layout)
          ((:layout-func layout) entity
                                 component
                                 (get @context (:name layout))))))))

(defn initialize [app-state]
  (b/on app-state ["layouts.do"] -999 (fn [event]
                                        (when-let [{:keys [container]} (:context event)]
                                          (do-layouts container)
                                          (b/fire app-state "uncommited.render")
                                          (b/fire app-state "rendering.finish")))))
