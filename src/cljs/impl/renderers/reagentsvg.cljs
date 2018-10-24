(ns impl.renderers.reagentsvg
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]
            [core.rendering :as r]
            [core.utils.dom :as dom]
            [reagent.core :as reagent :refer [atom]]
            [impl.components :as impld]))

(defonce svg-property-mapping {:left "x"
                               :top  "y"
                               :round-x "rx"
                               :round-y "ry"
                               :width  "width"
                               :height "height"
                               :angle "angle"
                               :x1 "x1"
                               :y1 "y1"
                               :x2 "x2"
                               :y2 "y2"
                               :border-color  "stroke"
                               :background-color "fill"
                               :radius "r"
                               :font-family "font-family"
                               :font-weight "font-weight"
                               :font-size "font-size"
                               :text-align "text-align"
                               :visible "visible"
                               :color "stroke"
                               :border-width "stroke-width"})

(defonce reactive-svgs (atom {}))

(defonce constants-bindings {:top 100000
                             :bottom 0})
(defn- resolve-value [val]
 (if (keyword? val)
   (or (val constants-bindings) val)
   val))

(defn- model-attributes [component]
  (let [model (d/model component)]
    (apply merge (mapv (fn [e] {e (resolve-value (e model))}) (keys model)))))

(defn- svg-shape-attributes
  ([component-model]
    (apply merge (mapv (fn [e] {(keyword (or (e svg-property-mapping) e)) (resolve-value (e component-model))}) (keys component-model))))
  ([component-model attributes]
    (apply merge (mapv (fn [e] {(keyword (or (e svg-property-mapping) e)) (resolve-value (e component-model))}) attributes))))

(defn- attributes-sync [component rendering-context]
  (let [source  (:data (r/get-state-of component))
        component-model (model-attributes component)
        properties  (get-in rendering-context [:redraw-properties (:uid component)])
        svg-attributes (svg-shape-attributes component-model properties)
        old-svg-attributes (get-in @reactive-svgs [(:uid component) :dom 1])]
    (swap! reactive-svgs assoc-in [(:uid component) :dom 1] (merge old-svg-attributes svg-attributes))))

(defn- z-index-sorted []
  (sort-by #(-> % :attributes :z-index) (vals @reactive-svgs)))

(defn Root [dom-id width height]
  [:svg {:id (str dom-id "-svg") :width width :height height}
    (doall
      (for [svg (z-index-sorted)]
        ^{:key (-> svg :attributes :id)}
        (:dom svg)))])

;;==========================================================================================================
;; rendering context initialization
;;==========================================================================================================
(defmethod r/initialize :reagentsvg [dom-id width height]
  (reagent/render-component [Root (str dom-id "-svg") width height]
    (dom/by-id dom-id)))

(defmethod r/all-rendered :reagentsvg [context]
  (console.log "all-rendered: SVG renderer does not support this type of method."))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-rect] [component context]
  (attributes-sync component context))

(defmethod r/create-rendering-state [:reagentsvg :draw-rect] [component context]
  (let [model (model-attributes component)
        attributes (svg-shape-attributes model)
        state {:dom  [:rect (merge {:id (:uid component)} attributes)] :attributes model}]
    (swap! reactive-svgs assoc (:uid component) state)
    {:data state}))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-rect] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; startpoint rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-circle] [component context]
  (attributes-sync component context))

(defmethod r/create-rendering-state [:reagentsvg :draw-circle] [component context]
  (let [model (model-attributes component)
        attributes (svg-shape-attributes model)
        state {:dom  [:circle (merge {:id (:uid component)} attributes)] :attributes model}]
    (swap! reactive-svgs assoc (:uid component) state)
    {:data state}))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-circle] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-line] [component rendering-context]
  (attributes-sync component context))

(defmethod r/create-rendering-state [:reagentsvg :draw-line] [component context]
  (let [model (model-attributes component)
        attributes (svg-shape-attributes model)
        state {:dom  [:line (merge {:id (:uid component)} attributes)] :attributes model}]
    (swap! reactive-svgs assoc (:uid component) state)
    {:data state}))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-line] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-triangle] [component context]
  (console.log "do-render :draw-triangle has been not yet implemented."))

(defmethod r/create-rendering-state [:reagentsvg :draw-triangle] [component context]
  (console.log "create-rendering-state :draw-triangle has been not yet implemented."))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-triangle] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-text] [component context]
  (attributes-sync component context)) ;;sync text attribute !!!

(defmethod r/create-rendering-state [:reagentsvg :draw-text] [component context]
  (let [model (model-attributes component)
        attributes (svg-shape-attributes model)
        state {:dom  [:text (merge {:id (:uid component)} attributes) (:text model)] :attributes model}]
    (swap! reactive-svgs assoc (:uid component) state)
    {:data state}))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-text] [component context]
  (swap! reactive-svgs dissoc (:uid component)))
