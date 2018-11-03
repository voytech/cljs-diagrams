(ns impl.renderers.reagentsvg
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.entities :as e]
            [core.eventbus :as b]
            [core.rendering :as r]
            [core.utils.dom :as dom]
            [reagent.core :as reagent :refer [atom]]
            [impl.components :as impld]))

(defonce svg-property-mapping {:left :x
                               :top  :y
                               :round-x :rx
                               :round-y :ry
                               :width  :width
                               :height :height
                               :angle {:to :transform :eval (fn [val mdl] (str "rotate(" val "," (:left mdl) "," (:top mdl) ")" ))}
                               :x1 :x1
                               :y1 :y1
                               :x2 :x2
                               :y2 :y2
                               :border-color  :stroke
                               :background-color :fill
                               :radius :r
                               :font-family :font-family
                               :font-weight :font-weight
                               :font-size :font-size
                               :text-align :text-align
                               :visible {:to :visibility :eval (fn [val mdl] (if (== val true) "visible" "hidden"))}
                               :color :stroke
                               :border-width :stroke-width})

(defonce reactive-svgs (atom {}))

(defonce pending-measures (atom []))

(defonce constants-bindings {:top 100000
                             :bottom 0})

(defn- resolve-value [val]
 (if (keyword? val)
   (or (val constants-bindings) val)
    val))

(defn- model-attributes [component]
  (let [model (d/model component)]
    (apply merge (mapv (fn [e] {e (resolve-value (e model))}) (keys model)))))

(defn- resolve-attribute [attrib-key model]
  (let [attrib-val (attrib-key model)
        svg-attrib (or (attrib-key svg-property-mapping) attrib-key)]
    (if (map? svg-attrib)
       {(:to svg-attrib) ((:eval svg-attrib) (resolve-value attrib-val) model)}
       {svg-attrib (resolve-value attrib-val)})))

(defn- svg-shape-attributes
  ([component-model]
    (apply merge (mapv (fn [e] (resolve-attribute e component-model)) (keys component-model))))
  ([component-model attributes]
    (apply merge (mapv (fn [e] (resolve-attribute e component-model)) attributes))))

(defn- attributes-sync
  ([component rendering-context computed-attributes]
    (let [source  (:data (r/get-state-of component))
          component-model (model-attributes component)
          properties  (get-in rendering-context [:redraw-properties (:uid component)])
          svg-attributes (svg-shape-attributes component-model properties)
          old-svg-attributes (get-in @reactive-svgs [(:uid component) :dom 1])]
      (swap! reactive-svgs assoc-in [(:uid component) :dom 1] (merge old-svg-attributes svg-attributes computed-attributes))))
  ([component rendering-context] (attributes-sync component rendering-context {})))

(defn- z-index-sorted []
  (sort-by #(-> % :attributes :z-index) (vals @reactive-svgs)))

(defn- measure-text []
  (doseq [id @pending-measures]
    (let [element (dom/by-id id)
          bbox (.getBBox element)]
      (console.log bbox)))
  (reset! pending-measures []))

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
;; circle rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-circle] [component context]
  (let [source  (:data (r/get-state-of component))
        component-model (model-attributes component)
        properties  (get-in context [:redraw-properties (:uid component)])
        svg-attributes (svg-shape-attributes component-model properties)
        circle-attribs (merge svg-attributes {
          "cx" (+ (:left component-model) (:radius component-model))
          "cy" (+ (:top component-model) (:radius component-model))})
        old-svg-attributes (get-in @reactive-svgs [(:uid component) :dom 1])]
    (swap! reactive-svgs assoc-in [(:uid component) :dom 1] (merge old-svg-attributes circle-attribs))))

(defmethod r/create-rendering-state [:reagentsvg :draw-circle] [component context]
  (let [model (model-attributes component)
        attributes (svg-shape-attributes model)
        circle-attribs (merge attributes {
          "cx" (+ (:left model) (:radius model))
          "cy" (+ (:top model) (:radius model))})
        state {:dom  [:circle (merge {:id (:uid component)} circle-attribs)] :attributes model}]
    (swap! reactive-svgs assoc (:uid component) state)
    {:data state}))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-circle] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; line rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-line] [component context]
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
(defn- path-d-from-pos [model]
  (let [x (:left model)
        y (:top model)
        width (:width model)
        height (:height model)]
   (str "M " (- x (/ width 2)) "," (+ y (/ height 2)) " "
             (+ x (/ width 2)) "," (+ y (/ height 2)) " "
              x "," (- y (/ height 2))
        " z")))

(defmethod r/do-render [:reagentsvg :draw-triangle] [component context]
  (attributes-sync component context {:d (path-d-from-pos (model-attributes component))}))

(defmethod r/create-rendering-state [:reagentsvg :draw-triangle] [component context]
  (let [model (model-attributes component)
        attributes (svg-shape-attributes model)
        state {:dom  [:path (merge {:id (:uid component) :d (path-d-from-pos model)} attributes)] :attributes model}]
    (swap! reactive-svgs assoc (:uid component) state)
    {:data state}))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-triangle] [component context]
  (swap! reactive-svgs dissoc (:uid component)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:reagentsvg :draw-text] [component context]
  (swap! pending-measures conj (:uid component))
  (attributes-sync component context))

(defmethod r/create-rendering-state [:reagentsvg :draw-text] [component context]
  (swap! pending-measures conj (:uid component))
  (let [model (model-attributes component)
        attributes (svg-shape-attributes model)
        state { :dom  [:text (merge {:id (:uid component)} attributes) (:text model)]
                :attributes model }]
    (swap! reactive-svgs assoc (:uid component) state)
    {:data state}))

(defmethod r/destroy-rendering-state [:reagentsvg :draw-text] [component context]
  (swap! reactive-svgs dissoc (:uid component)))
