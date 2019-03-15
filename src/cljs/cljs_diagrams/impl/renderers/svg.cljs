(ns cljs-diagrams.impl.renderers.svg
  (:require [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.rendering :as r]
            [cljs-diagrams.core.state :as state]
            [cljs-diagrams.core.utils.dom :as dom]
            [cljs-diagrams.core.utils.svg :as s]
            [cljs-diagrams.core.eventbus :as bus]))

(defn- simple-set [to]
  (fn [svg val mdl]
    (dom/attr svg (name to) val)))

(defonce svg-property-mapping {:left (simple-set :x)
                               :top  (simple-set :y)
                               :round-x (simple-set :rx)
                               :round-y (simple-set :ry)
                               :width  (simple-set :width)
                               :height (simple-set :height)
                               :points (fn [svg val mdl]
                                          (dom/attr svg "points"
                                            (reduce (fn [agg point]
                                                      (str agg " " (nth point 0) "," (nth point 1)))
                                                    ""
                                                    (partition 2 val))))
                               :svg-path (simple-set :d)
                               :angle  (fn [svg val mdl]
                                          (dom/attr svg "transform"
                                            (str "rotate(" val "," (+ (:left mdl) (/ (:width mdl) 2)) "," (+ (:top mdl) (/ (:height mdl) 2)) ")")))
                               :x1 (simple-set :x1)
                               :y1 (simple-set :y1)
                               :x2 (simple-set :x2)
                               :y2 (simple-set :y2)
                               :border-color  (simple-set :stroke)
                               :background-color (simple-set :fill)
                               :radius (simple-set :r)
                               :opacity (simple-set :fill-opacity)
                               ;; Text attributes
                               :font-family (simple-set :font-family)
                               :font-weight (simple-set :font-weight)
                               :font-size (simple-set :font-size)
                               :text-align (simple-set :text-align)
                               :text (fn [svg val mdl]
                                       (dom/attr svg "dominant-baseline" "hanging")
                                       (dom/set-text svg val))
                               ;; ---
                               :visible (fn [svg val mdl] (dom/attr svg "visibility" (if (== val true) "visible" "hidden")))
                               :color (simple-set :stroke)
                               :border-style (simple-set :stroke-style)
                               :stroke-style (fn [svg val mdl]
                                              (cond
                                                (= val :dashed) (dom/attr svg "stroke-dasharray" "5,5")
                                                :else           (dom/attr svg "stroke-dasharray" "none")))
                               :border-width (simple-set :stroke-width)
                               :image-url (simple-set :href)})

(defonce constants-bindings {:top 100000
                             :before-top 99999
                             :before-bottom 1
                             :bottom 0})

(defn- resolve-value [val]
 (if (keyword? val)
   (or (val constants-bindings) val)
   val))

(defn- model-attributes [shape]
  (let [model (d/model shape)]
    (apply merge (mapv (fn [e] {e (resolve-value (e model))}) (keys model)))))

(defn- sync-svg-element
  ([svg shape-model]
   (sync-svg-element svg shape-model (keys shape-model)))
  ([svg shape-model attributes]
   (doseq [attribute attributes]
     (let [func (attribute svg-property-mapping)]
       (when (not (nil? func))
         (func svg (resolve-value (attribute shape-model)) shape-model))))))

(defn- update-svg-element [renderer-state shape properties postprocess]
  (let [source  (dom/by-id (:uid shape))
        model  (model-attributes shape)]
    (when (some #(= :z-index %) properties)
      (refresh-z-index shape))
    (sync-svg-element source model properties)
    (when (not (nil? postprocess)) (postprocess source))))

(defn- create-svg-element [renderer-state svg-name shape postprocess]
  (let [root (dom/by-id (:root renderer-state))
        source (s/create-element svg-name root {})
        model (model-attributes shape)]
      (sync-svg-element source model)
      (dom/attr source "id" (:uid shape))
      (dom/attr source "data-z-index" (resolve-value (d/getp shape :z-index)))
      (when (not (nil? postprocess)) (postprocess source))))

(defn- refresh-z-index [shape]
  (let [node (dom/by-id (:uid shape))
        parent (.-parentNode node)
        count (dom/child-count parent)
        z-index (resolve-value (d/getp shape :z-index))]
    (when (not=  z-index (dom/attr node "data-z-index"))
      (doseq [prev-dom (mapv #(dom/get-child-at parent %) (reverse (range count)))]
        (when (not= (dom/attr prev-dom "id") (:uid shape))
          (let [prev-dom-z-index (dom/attr prev-dom "data-z-index")]
            (when (> prev-dom-z-index z-index)
              (dom/insert-before node prev-dom))))))))

(defn- measure-text [shape]
  (when-let [domnode (dom/by-id (:uid shape))]
    (let [bbox (.getBBox domnode)]
      (d/set-data shape {:width (.-width bbox) :height (.-height bbox)}))))

(defmethod r/is-state-created :svg [renderer-state shape]
   (some? (dom/by-id (:uid shape))))
;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :svg [renderer dom-id width height]
  (s/create-svg dom-id "svg" {:width width :height height})
  {:root (str dom-id "-svg")})

(defmethod r/all-rendered :svg [state context])
;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-rect] [renderer-state shape properties]
  (update-svg-element renderer-state shape properties nil))

(defmethod r/create-rendering-state [:svg :draw-rect] [renderer-state shape]
  (create-svg-element renderer-state "rect" shape nil))

(defmethod r/destroy-rendering-state [:svg :draw-rect] [renderer-state shape]
  (dom/remove-by-id (:uid shape)))

;;==========================================================================================================
;; circle rendering
;;==========================================================================================================
(defn- circle [shape]
  (fn [svg]
    (let [mdl (model-attributes shape)]
      (dom/attrs svg {"cx" (+ (:left mdl ) (:radius mdl))
                      "cy" (+ (:top  mdl ) (:radius mdl))}))))

(defmethod r/do-render [:svg :draw-circle] [renderer-state shape properties]
  (update-svg-element renderer-state shape properties (circle shape)))

(defmethod r/create-rendering-state [:svg :draw-circle] [renderer-state shape]
  (create-svg-element renderer-state "circle" shape (circle shape)))

(defmethod r/destroy-rendering-state [:svg :draw-circle] [renderer-state shape]
  (dom/remove-by-id (:uid shape)))

;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-line] [renderer-state shape properties]
  (update-svg-element renderer-state shape properties nil))

(defmethod r/create-rendering-state [:svg :draw-line] [renderer-state shape]
  (create-svg-element renderer-state "line" shape nil))

(defmethod r/destroy-rendering-state [:svg :draw-line] [renderer-state shape]
  (dom/remove-by-id (:uid shape)))

;;==========================================================================================================
;; svg path rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-svg-path] [renderer-state shape properties]
  (update-svg-element renderer-state shape properties nil))

(defmethod r/create-rendering-state [:svg :draw-svg-path] [renderer-state shape]
  (create-svg-element renderer-state "path" shape nil))

(defmethod r/destroy-rendering-state [:svg :draw-svg-path] [renderer-state shape]
  (dom/remove-by-id (:uid shape)))

;;==========================================================================================================
;; poly-line rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-poly-line] [renderer-state shape properties]
  (update-svg-element renderer-state shape properties nil))

(defmethod r/create-rendering-state [:svg :draw-poly-line] [renderer-state shape]
  (create-svg-element renderer-state "polyline" shape nil))

(defmethod r/destroy-rendering-state [:svg :draw-poly-line] [renderer-state shape]
  (dom/remove-by-id (:uid shape)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-text] [renderer-state shape properties]
  (update-svg-element renderer-state shape properties nil)
  (measure-text shape))

(defmethod r/create-rendering-state [:svg :draw-text] [renderer-state shape]
  (create-svg-element renderer-state "text" shape nil))

(defmethod r/destroy-rendering-state [:svg :draw-text] [renderer-state shape]
  (dom/remove-by-id (:uid shape)))


;;==========================================================================================================
;; image rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-image] [renderer-state shape properties]
  (update-svg-element renderer-state shape properties nil))

(defmethod r/create-rendering-state [:svg :draw-image] [renderer-state shape]
  (create-svg-element renderer-state "image" shape nil))

(defmethod r/destroy-rendering-state [:svg :draw-image] [renderer-state shape]
  (dom/remove-by-id (:uid shape)))
