(ns cljs-diagrams.impl.renderers.svg
  (:require [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.entities :as e]
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

(defn- model-attributes [component]
  (let [model (d/model component)]
    (apply merge (mapv (fn [e] {e (resolve-value (e model))}) (keys model)))))

(defn- sync-svg-element
  ([svg component-model]
   (sync-svg-element svg component-model (keys component-model)))
  ([svg component-model attributes]
   (doseq [attribute attributes]
     (let [func (attribute svg-property-mapping)]
       (when (not (nil? func))
         (func svg (resolve-value (attribute component-model)) component-model))))))

(defn- update-svg-element [renderer-state component properties postprocess]
  (let [source  (dom/by-id (:uid component))
        model  (model-attributes component)]
    (when (some #(= :z-index %) properties)
      (refresh-z-index component))
    (sync-svg-element source model properties)
    (when (not (nil? postprocess)) (postprocess source))))

(defn- create-svg-element [renderer-state svg-name component postprocess]
  (let [root (dom/by-id (:root renderer-state))
        source (s/create-element svg-name root {})
        model (model-attributes component)]
      (sync-svg-element source model)
      (dom/attr source "id" (:uid component))
      (dom/attr source "data-z-index" (resolve-value (d/getp component :z-index)))
      (when (not (nil? postprocess)) (postprocess source))))

(defn- refresh-z-index [component]
  (let [node (dom/by-id (:uid component))
        parent (.-parentNode node)
        count (dom/child-count parent)
        z-index (resolve-value (d/getp component :z-index))]
    (when (not=  z-index (dom/attr node "data-z-index"))
      (doseq [prev-dom (mapv #(dom/get-child-at parent %) (reverse (range count)))]
        (when (not= (dom/attr prev-dom "id") (:uid component))
          (let [prev-dom-z-index (dom/attr prev-dom "data-z-index")]
            (when (> prev-dom-z-index z-index)
              (dom/insert-before node prev-dom))))))))

(defn- measure-text [component]
  (when-let [domnode (dom/by-id (:uid component))]
    (let [bbox (.getBBox domnode)]
      (d/set-data component {:width (.-width bbox) :height (.-height bbox)}))))

(defmethod r/is-state-created :svg [renderer-state component]
   (some? (dom/by-id (:uid component))))
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
(defmethod r/do-render [:svg :draw-rect] [renderer-state component properties]
  (update-svg-element renderer-state component properties nil))

(defmethod r/create-rendering-state [:svg :draw-rect] [renderer-state component]
  (create-svg-element renderer-state "rect" component nil))

(defmethod r/destroy-rendering-state [:svg :draw-rect] [renderer-state component]
  (dom/remove-by-id (:uid component)))

;;==========================================================================================================
;; circle rendering
;;==========================================================================================================
(defn- circle [component]
  (fn [svg]
    (let [mdl (model-attributes component)]
      (dom/attrs svg {"cx" (+ (:left mdl ) (:radius mdl))
                      "cy" (+ (:top  mdl ) (:radius mdl))}))))

(defmethod r/do-render [:svg :draw-circle] [renderer-state component properties]
  (update-svg-element renderer-state component properties (circle component)))

(defmethod r/create-rendering-state [:svg :draw-circle] [renderer-state component]
  (create-svg-element renderer-state "circle" component (circle component)))

(defmethod r/destroy-rendering-state [:svg :draw-circle] [renderer-state component]
  (dom/remove-by-id (:uid component)))

;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-line] [renderer-state component properties]
  (update-svg-element renderer-state component properties nil))

(defmethod r/create-rendering-state [:svg :draw-line] [renderer-state component]
  (create-svg-element renderer-state "line" component nil))

(defmethod r/destroy-rendering-state [:svg :draw-line] [renderer-state component]
  (dom/remove-by-id (:uid component)))

;;==========================================================================================================
;; svg path rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-svg-path] [renderer-state component properties]
  (update-svg-element renderer-state component properties nil))

(defmethod r/create-rendering-state [:svg :draw-svg-path] [renderer-state component]
  (create-svg-element renderer-state "path" component nil))

(defmethod r/destroy-rendering-state [:svg :draw-svg-path] [renderer-state component]
  (dom/remove-by-id (:uid component)))

  ;;==========================================================================================================
  ;; poly-line rendering
  ;;==========================================================================================================
(defmethod r/do-render [:svg :draw-poly-line] [renderer-state component properties]
  (update-svg-element renderer-state component properties nil))

(defmethod r/create-rendering-state [:svg :draw-poly-line] [renderer-state component]
  (create-svg-element renderer-state "polyline" component nil))

(defmethod r/destroy-rendering-state [:svg :draw-poly-line] [renderer-state component]
  (dom/remove-by-id (:uid component)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-text] [renderer-state component properties]
  (update-svg-element renderer-state component properties nil)
  (measure-text component))

(defmethod r/create-rendering-state [:svg :draw-text] [renderer-state component]
  (create-svg-element renderer-state "text" component nil))

(defmethod r/destroy-rendering-state [:svg :draw-text] [renderer-state component]
  (dom/remove-by-id (:uid component)))


;;==========================================================================================================
;; image rendering
;;==========================================================================================================
(defmethod r/do-render [:svg :draw-image] [renderer-state component properties]
  (update-svg-element renderer-state component properties nil))

(defmethod r/create-rendering-state [:svg :draw-image] [renderer-state component]
  (create-svg-element renderer-state "image" component nil))

(defmethod r/destroy-rendering-state [:svg :draw-image] [renderer-state component]
  (dom/remove-by-id (:uid component)))
