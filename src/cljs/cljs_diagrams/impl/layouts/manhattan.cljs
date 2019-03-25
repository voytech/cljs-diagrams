(ns cljs-diagrams.impl.layouts.manhattan
  (:require [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.layouts :as l]
            [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.behaviour-api :as api]
            [cljs-diagrams.impl.std.behaviours.behaviour-api :as std]
            [cljs-diagrams.impl.std.shapes :as c]))

(declare head-position-hints)
(declare tail-position-hints)
(declare set-head-position)
(declare set-tail-position)

(defn manhattan-bbox-apply [app-state node startpoint endpoint]
(let [node  (e/reload app-state node)]
    (when (and (some? startpoint) (some? endpoint))
      (e/set-bbox app-state
                  node
                  {:left   (if (< (:x startpoint) (:x endpoint)) (:x startpoint) (:x endpoint))
                   :top    (if (< (:y startpoint) (:y endpoint)) (:y startpoint) (:y endpoint))
                   :width  (js/Math.abs (- (:x endpoint) (:x startpoint)))
                   :height (js/Math.abs (- (:y endpoint) (:y startpoint)))}))))

(defn- center-point [cmp]
  (let [mx (+ (d/get-left cmp) (/ (d/get-width cmp) 2))
        my (+ (d/get-top cmp) (/ (d/get-height cmp) 2))]
    {:x mx :y my}))

(defn eval-vectors [start end]
  (if (> (- (:x end) (:x start)) (- (:y end) (:y start))) [:h :v] [:v :h]))

(defn- find-connection-points [sp ep s-normal e-normal]
  (let [dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
    (cond
      (and (= :h e-normal) (= :h s-normal)) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
      (and (= :v e-normal) (= :v s-normal)) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
      (and (= :v s-normal) (= :h e-normal)) [{:x (:x sp) :y (:y ep)}]
      (and (= :h s-normal) (= :v e-normal)) [{:x (:x ep) :y (:y sp)}])))

(defn- distance [p1 p2]
  (js/Math.sqrt (+ (js/Math.pow (- (:x p2) (:x p1)) 2) (js/Math.pow (- (:y p2) (:y p1)) 2))))

(defn- find-path [start end s-normal e-normal]
  (find-connection-points start end s-normal e-normal))

(defn to-polyline-points [start end points]
  (->  (mapv (fn [entry] [(:x entry) (:y entry)]) (flatten [start points end]))
       (flatten)))

(defn- calculate-vectors [app-state
                          source-node source-control
                          target-node target-control]
  (let [source-control-side (e/shape-attribute app-state source-node (:name source-control) :side)
        target-control-side (e/shape-attribute app-state target-node (:name target-control) :side)]
    [(if (or (= :left source-control-side) (= :right source-control-side)) :h :v)
     (if (or (= :left target-control-side) (= :right target-control-side)) :h :v)]))

(defn nearest-control [node point]
  (let [src-connectors (e/get-node-shape node ::c/control)]
    (apply min-key #(distance (center-point %) point) src-connectors)))

(defn- nearest-controls-between [app-state src-node trg-node]
  (let [src-connectors (e/get-node-shape src-node ::c/control)
        trg-connectors (e/get-node-shape trg-node ::c/control)]
    (->> (for [src src-connectors
               trg trg-connectors]
           {:src src :trg trg :d (distance (center-point src) (center-point trg))})
         (apply min-key #(:d %)))))

(defn move-point [pos movement-x movement-y]
  {:x (+ (:x pos) movement-x) :y (+ (:y pos) movement-y)})

(defn manhattan-coords [tail-pos head-pos]
  (let [normals  (eval-vectors tail-pos head-pos)]
    {:tail tail-pos :head head-pos :vectors normals}))

(defn head-position-hints
  ([shape]
   (-> shape :layout-attributes :layout-hints :head-position))
  ([app-state shape]
   (-> (e/shape-layout-attributes app-state shape)
       :layout-hints
       :head-position)))

(defn tail-position-hints
  ([shape]
   (-> shape :layout-attributes :layout-hints :tail-position))
  ([app-state shape]
   (-> (e/shape-layout-attributes app-state shape)
       :layout-hints
       :tail-position)))

(defn is-relation-shape [shape]
  (and (= (:rendering-method shape) :draw-poly-line)
       (= (:type shape) ::c/relation)))

(defn is-head-decorator-shape [shape]
  (let [hints (-> shape :layout-attributes :layout-hints)]
    (true? (:decorates-head hints))))

(defn is-tail-decorator-shape [shape]
  (let [hints (-> shape :layout-attributes :layout-hints)]
    (true? (:decorates-tail hints))))

(defn has-position-hints? [shape]
  (and (some? (-> shape :layout-attributes :layout-hints :head-position))
       (some? (-> shape :layout-attributes :layout-hints :tail-position))))

(defn create-manhattan-layout-context [app-state node]
  (let [source-node (first (e/get-related-nodes app-state node :start))
        target-node (first (e/get-related-nodes app-state node :end))
        shape       (first (e/get-node-shape app-state node ::c/relation))
        relation-head (head-position-hints shape)
        relation-tail (tail-position-hints shape)]
      (cond
         (and (some? source-node) (nil? target-node))
         (manhattan-coords (-> (nearest-control source-node relation-head)
                               (center-point))
                           relation-head)
         (and (some? target-node) (nil? source-node))
         (manhattan-coords relation-tail
                           (-> (nearest-control target-node relation-tail)
                               (center-point)))
         (and (some? source-node) (some? target-node))
         (let [{:keys [src trg]} (nearest-controls-between app-state source-node target-node)
               vectors (calculate-vectors app-state source-node src target-node trg)]
           {:tail (center-point src) :head (center-point trg) :vectors vectors})
         :else
         (manhattan-coords relation-tail relation-head))))

(defn layout-relation [app-state node shape context]
  (let [{:keys [tail head vectors]} (:layout-coords context)]
    (let [points (find-path tail head (vectors 0) (vectors 1))
          polyline-points (to-polyline-points tail head points)]
      {:processing-context context
       :to-set {
           :points polyline-points
           }})))

(defn layout-head-decorator [app-state node shape context]
  (let [end-pos   (std/get-relation-end node)
        relation  (first (e/get-node-shape node ::c/relation))
        {:keys [x1 y1 x2 y2]} (std/head-vector relation)]
    {:processing-context context
     :to-set {
         :left (- (:x end-pos) (/ (d/get-width shape) 2))
         :top (- (:y end-pos) (/ (d/get-height shape) 2))
         :angle (std/calculate-angle x1 y1 x2 y2)
         }}))

(defn layout-tail-decorator [app-state node shape context]
  (let [start-pos (std/get-relation-start node)
        relation  (first (e/get-node-shape node ::c/relation))
        {:keys [x1 y1 x2 y2]} (std/tail-vector relation)]
    {:processing-context context
     :to-set {
         :left (- (:x start-pos) (/ (d/get-width shape) 2))
         :top (- (:y start-pos) (/ (d/get-height shape) 2))
         :angle (std/calculate-angle x1 y1 x2 y2)
         }}))

(defmethod l/create-context ::manhattan [app-state node layout]
  (let [context (create-manhattan-layout-context app-state node)]
    (manhattan-bbox-apply app-state node (:tail context) (:head context))
    {:layout-coords context}))

(defmethod l/layout-function ::manhattan [node shape context]
  (let [app-state (:app-state context)
        cbbox (d/get-bbox shape)]
    (cond
      (is-relation-shape shape)
      (layout-relation app-state node shape context)
      (is-head-decorator-shape shape)
      (layout-head-decorator app-state node shape context)
      (is-tail-decorator-shape shape)
      (layout-tail-decorator app-state node shape context))))

(defn manhattan-head-decorator-attributes [name]
  (d/layout-attributes name 1 {:decorates-head true}))

(defn manhattan-tail-decorator-attributes [name]
  (d/layout-attributes name 1 {:decorates-tail true}))

(defn manhattan-relation-attributes [name]
  (d/layout-attributes name 0 {:relation true}))

(defn set-head-position
  ([app-state node position]
   (let [relation (first (e/get-node-shape app-state node ::c/relation))]
     (e/set-shape-layout-hint app-state relation :head-position position)))
  ([app-state node movement-x movement-y]
   (let [relation (first (e/get-node-shape app-state node ::c/relation))
         pos (head-position-hints app-state relation)
         new-pos {:x (+ (:x pos) movement-x)
                  :y (+ (:y pos) movement-y)}]
     (set-head-position app-state node new-pos))))

(defn set-tail-position
  ([app-state node position]
   (let [relation (first (e/get-node-shape app-state node ::c/relation))]
     (e/set-shape-layout-hint app-state relation :tail-position position)))
  ([app-state node movement-x movement-y]
   (let [relation (first (e/get-node-shape app-state node ::c/relation))
         pos (tail-position-hints app-state relation)
         new-pos {:x (+ (:x pos) movement-x)
                  :y (+ (:y pos) movement-y)}]
     (set-tail-position app-state node new-pos))))
