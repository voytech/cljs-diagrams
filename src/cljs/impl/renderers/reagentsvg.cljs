(ns impl.renderers.reagentsvg
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]
            [core.rendering :as r]
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
                               :text-align (simple-set :text-align)
                               :text (fn [svg val mdl] (assoc svg 2 val))
                               :visible (fn [svg val mdl] (assoc-in svg [1 :visibility] (if (== val true) "visible" "hidden")))
                               :color (simple-set :stroke)
                               :border-style (simple-set :stroke-style)
                               :border-width (simple-set :stroke-width)})

(defonce reactive-svgs (atom {}))

(defonce pending-measures (atom {}))

(defonce constants-bindings {:top 100000
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

(defn- update-svg-element [component rendering-context postprocess]
  (let [source  (get-in @reactive-svgs [(:uid component) :dom])
        model (model-attributes component)
        properties  (get-in rendering-context [:redraw-properties (:uid component)])
        svg (sync-svg-element source model properties)]
    (swap! reactive-svgs assoc (:uid component)
      {:dom (if (not (nil? postprocess)) (postprocess svg) svg)
       :attributes model})))

(defn- create-svg-element [svg-name component rendering-context postprocess]
  (let [source [svg-name {:id (:uid component)}]
        model (model-attributes component)
        svg (sync-svg-element source model)]
    (swap! reactive-svgs assoc (:uid component)
      {:dom (if (not (nil? postprocess)) (postprocess svg) svg)
       :attributes model})))

(defn- z-index-sorted []
  (sort-by #(-> % :attributes :z-index) (vals @reactive-svgs)))

(defn- request-text-measure [component]
  (swap! pending-measures assoc (:uid component) component))

(defn- close-text-measure [id]
  (swap! pending-measures dissoc id))

(defn- measure-text []
  (doseq [id (keys @pending-measures)]
    (let [domnode (dom/by-id id)
          component (get @pending-measures id)
          bbox (.getBBox domnode)]
      (d/set-data component {:width (.-width bbox) :height (.-height bbox)})
      (close-text-measure id)
      (bus/fire "layout.do" {:container (e/lookup component :entity) :type :attributes}))))

(defn Root [dom-id width height]
  (reagent/create-class {
    :display-name  "Root"
    :component-did-update
        (fn [this old-argv] (measure-text))
    :reagent-render (fn [dom-id width height]
                      [:svg {:id (str dom-id "-svg") :width width :height height}
                        (doall
                          (for [svg (z-index-sorted)]
                            ^{:key (-> svg :attributes :id)}
                            (:dom svg)))])
                    }))

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :reagentsvg [dom-id width height]
  (reagent/render-component [Root (str dom-id "-svg") width height]
    (dom/by-id dom-id)))

(defmethod r/all-rendered :reagentsvg [context])
;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-rect] [component context]
  (update-svg-element component context nil))

(defmethod r/create-rendering-state [:reagentsvg :draw-rect] [component context]
  {:data (create-svg-element :rect component context nil)})

(defmethod r/destroy-rendering-state [:reagentsvg :draw-rect] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; circle rendering
;;==========================================================================================================

(defn- circle [component]
  (fn [svg]
    (let [mdl (model-attributes component)]
      (-> svg
         (assoc-in [1 :cx] (+ (:left mdl ) (:radius mdl)) )
         (assoc-in [1 :cy] (+ (:top  mdl ) (:radius mdl)) ) ))))

(defmethod r/do-render [:reagentsvg :draw-circle] [component context]
  (update-svg-element component context (circle component)))


(defmethod r/create-rendering-state [:reagentsvg :draw-circle] [component context]
  {:data (create-svg-element :circle component context (circle component))})

(defmethod r/destroy-rendering-state [:reagentsvg :draw-circle] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-line] [component context]
  (update-svg-element component context nil))

(defmethod r/create-rendering-state [:reagentsvg :draw-line] [component context]
  {:data (create-svg-element :line component context nil)})

(defmethod r/destroy-rendering-state [:reagentsvg :draw-line] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

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

(defmethod r/do-render [:reagentsvg :draw-triangle] [component context]
  (update-svg-element component context (triangle component)))

(defmethod r/create-rendering-state [:reagentsvg :draw-triangle] [component context]
  {:data (create-svg-element :path component context (triangle component))})

(defmethod r/destroy-rendering-state [:reagentsvg :draw-triangle] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-text] [component context]
  (request-text-measure component)
  (update-svg-element component context nil))

(defmethod r/create-rendering-state [:reagentsvg :draw-text] [component context]
  {:data (create-svg-element :text component context nil)})

(defmethod r/destroy-rendering-state [:reagentsvg :draw-text] [component context]
  (close-text-measure (:uid component))
  (swap! reactive-svgs dissoc (:uid component)))
