(ns impl.renderers.reagentsvg
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]
            [core.rendering :as r]
            [core.state :as state]
            [core.utils.dom :as dom]
            [core.eventbus :as bus]
            [reagent.core :as reagent :refer [atom]]
            [impl.components :as impld]))

(defn- simple-set [to]
  (fn [svg val mdl]
    (assoc-in svg [1 to] val)))

(defonce svg-property-mapping {:left (simple-set :x)
                               :top  (simple-set :y)
                               :round-x (simple-set :rx)
                               :round-y (simple-set :ry)
                               :width  (simple-set :width)
                               :height (simple-set :height)
                               :angle  (fn [svg val mdl]
                                          (assoc-in svg [1 :transform]
                                            (str "rotate(" val "," (:left mdl) "," (:top mdl) ")" )))
                               :x1 (simple-set :x1)
                               :y1 (simple-set :y1)
                               :x2 (simple-set :x2)
                               :y2 (simple-set :y2)
                               :border-color  (simple-set :stroke)
                               :background-color (simple-set :fill)
                               :radius (simple-set :r)
                               :font-family (simple-set :font-family)
                               :font-weight (simple-set :font-weight)
                               :font-size (simple-set :font-size)
                               :opacity (simple-set :fill-opacity)
                               :text-align (simple-set :text-align)
                               :text (fn [svg val mdl] (assoc svg 2 val))
                               :visible (fn [svg val mdl] (assoc-in svg [1 :visibility] (if (== val true) "visible" "hidden")))
                               :color (simple-set :stroke)
                               :border-style (simple-set :stroke-style)
                               :border-width (simple-set :stroke-width)
                               :image-url (simple-set :href)})

(defonce constants-bindings {:top 100000
                             :before-bottom 1
                             :bottom 0})

(defn- resolve-value [val]
 (if (keyword? val)
   (or (val constants-bindings) val)
    val))

(defn- model-attributes [component]
  (let [model (d/model component)]
    (apply merge (mapv (fn [e] {e (resolve-value (e model))}) (keys model)))))

(defn- resolve-attribute-wrapper [model]
  (fn [svg attribute]
    (let [func (attribute svg-property-mapping)]
       (if (not (nil? func))
         (func svg (resolve-value (attribute model)) model)
         svg))))

(defn- sync-svg-element
  ([svg component-model]
    (sync-svg-element svg component-model (keys component-model)))
  ([svg component-model attributes]
    (reduce (resolve-attribute-wrapper component-model) svg attributes)))

(defn- update-svg-element [renderer-state component postprocess]
  (let [rendering-component (get-in @renderer-state [:components (:uid component)])
        source (:dom rendering-component)
        model  (model-attributes component)
        properties (:redraw-properties rendering-component)
        svg (sync-svg-element source model properties)]
    (swap! renderer-state assoc-in [:components (:uid component)]
      {:dom (if (not (nil? postprocess)) (postprocess svg) svg)
       :attributes model})))

(defn- create-svg-element [renderer-state svg-name component postprocess]
  (let [source [svg-name {:id (:uid component)}]
        model (model-attributes component)
        svg (sync-svg-element source model)]
      {:dom (if (not (nil? postprocess)) (postprocess svg) svg)
       :attributes model}))

(defn- z-index-sorted [svg-components]
  (sort-by #(-> % :attributes :z-index) (vals svg-components)))

(defn- request-text-measure [renderer-state component]
  (swap! renderer-state assoc-in [:pending-measures (:uid component)] component))

(defn- close-text-measure [renderer-state id]
  (swap! renderer-state update-in [:pending-measures] dissoc id))

(defn- measure-text [app-state]
  (let [renderer-state (state/get-renderer-state app-state)]
    (doseq [id (-> renderer-state deref :pending-measures keys)]
      (let [domnode (dom/by-id id)
            component (get-in @renderer-state [:pending-measures id])
            bbox (.getBBox domnode)]
        (d/set-data component {:width (.-width bbox) :height (.-height bbox)})
        (close-text-measure renderer-state id)
        (bus/fire app-state "layouts.do" {:container (e/lookup app-state component)})))))

(defn Root [dom-id width height app-state renderer-state]
  (reagent/create-class {
    :display-name  "Root"
    :component-did-update
        (fn [this old-argv] (measure-text app-state))
    :reagent-render (fn [dom-id width height app-state renderer-state]
                      [:svg {:id (str dom-id "-svg") :width width :height height}
                        (doall
                          (for [svg (z-index-sorted (-> renderer-state deref :components))]
                            ^{:key (-> svg :attributes :id)}
                            (:dom svg)))])
                    }))

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :reagentsvg [renderer app-state dom-id width height initial-state]
  (let [renderer-state (atom initial-state)]
    (swap! renderer-state assoc :pending-measures {})
    (reagent/render-component [Root (str dom-id "-svg") width height app-state renderer-state] (dom/by-id dom-id))
    renderer-state))

(defmethod r/all-rendered :reagentsvg [state context])
;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-rect] [renderer-state component]
  (update-svg-element renderer-state component nil))

(defmethod r/create-rendering-state [:reagentsvg :draw-rect] [renderer-state component]
  (create-svg-element renderer-state :rect component nil))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-rect] [renderer-state component]
  (swap! renderer-state update :components dissoc (:uid component)))

;;==========================================================================================================
;; circle rendering
;;==========================================================================================================

(defn- circle [component]
  (fn [svg]
    (let [mdl (model-attributes component)]
      (-> svg
         (assoc-in [1 :cx] (+ (:left mdl ) (:radius mdl)) )
         (assoc-in [1 :cy] (+ (:top  mdl ) (:radius mdl)) ) ))))

(defmethod r/do-render [:reagentsvg :draw-circle] [renderer-state component]
  (update-svg-element renderer-state component (circle component)))


(defmethod r/create-rendering-state [:reagentsvg :draw-circle] [renderer-state component]
  (create-svg-element renderer-state :circle component (circle component)))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-circle] [renderer-state component]
  (swap! renderer-state update :components dissoc (:uid component)))

;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-line] [renderer-state component]
  (update-svg-element renderer-state component nil))

(defmethod r/create-rendering-state [:reagentsvg :draw-line] [renderer-state component]
  (create-svg-element renderer-state :line component nil))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-line] [renderer-state component]
  (swap! renderer-state update-in [:components] dissoc (:uid component)))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defn- triangle-from-pos [component]
  (let [model (model-attributes component)
        x (:left model)
        y (:top model)
        width (:width model)
        height (:height model)]
   (str "M " (- x (/ width 2)) "," (+ y (/ height 2)) " "
             (+ x (/ width 2)) "," (+ y (/ height 2)) " "
              x "," (- y (/ height 2))
        " z")))

(defn- triangle [component]
  (fn [svg]
    (assoc-in svg [1 :d] (triangle-from-pos component))))

(defmethod r/do-render [:reagentsvg :draw-triangle] [renderer-state component]
  (update-svg-element renderer-state component (triangle component)))

(defmethod r/create-rendering-state [:reagentsvg :draw-triangle] [renderer-state component]
  (create-svg-element renderer-state :path component (triangle component)))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-triangle] [renderer-state component]
  (swap! renderer-state update :components dissoc (:uid component)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-text] [renderer-state component]
  (let [properties (get-in @renderer-state [:components (:uid component) :redraw-properties])]
    (when (some #(= :text %) properties)
      (request-text-measure renderer-state component)))
  (update-svg-element renderer-state component nil))

(defmethod r/create-rendering-state [:reagentsvg :draw-text] [renderer-state component]
  (create-svg-element renderer-state :text component nil))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-text] [renderer-state component]
  (close-text-measure renderer-state (:uid component))
  (swap! renderer-state update :components dissoc (:uid component)))

;;==========================================================================================================
;; image rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-image] [renderer-state component]
  (update-svg-element renderer-state component nil))

(defmethod r/create-rendering-state [:reagentsvg :draw-image] [renderer-state component]
  (create-svg-element renderer-state :image component nil))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-image] [renderer-state component]
  (swap! renderer-state update :components dissoc (:uid component)))
