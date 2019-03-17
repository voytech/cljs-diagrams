(ns cljs-diagrams.impl.std.behaviours.manhattan
  (:require [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.layouts :as layouts]
            [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.behaviour-api :as api]
            [cljs-diagrams.impl.std.behaviours.behaviour-api :as std]
            [cljs-diagrams.impl.std.shapes :as c]))


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

(defn update-manhattan-layout [app-state node start end normals]
  (let [points (find-path start end (normals 0) (normals 1))
        polyline-points (to-polyline-points start end points)
        polyline (first (e/get-node-shape node ::c/relation))]
     (d/setp polyline :points polyline-points)))

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

(defn endpoint-move
  ([app-state node end-type new-active-pos]
   (let [node (e/node-by-id app-state (:uid node))
         tail-pos (if (= :start end-type)
                     new-active-pos
                     (std/get-relation-start node))
         head-pos (if (= :start end-type)
                     (std/get-relation-end node)
                     new-active-pos)
         vectors (eval-vectors tail-pos head-pos)]
     (update-manhattan-layout app-state node tail-pos head-pos vectors)
     (std/align-decorators (e/node-by-id app-state (:uid node)))
     (std/calc-association-bbox app-state node)))
  ([app-state node end-type movement-x movement-y]
   (let [node (e/node-by-id app-state (:uid node))
         tail-pos (if (= :start end-type)
                     (move-point (std/get-relation-start node) movement-x movement-y)
                     (std/get-relation-start node))
         head-pos (if (= :start end-type)
                     (std/get-relation-end node)
                     (move-point (std/get-relation-end node) movement-x movement-y))
         vectors (eval-vectors tail-pos head-pos)]
     (update-manhattan-layout app-state node tail-pos head-pos vectors)
     (std/align-decorators (e/node-by-id app-state (:uid node)))
     (std/calc-association-bbox app-state node))))

(defn manhattan-data [tail-pos head-pos]
  (let [normals  (eval-vectors tail-pos head-pos)]
    {:tail tail-pos :head head-pos :vectors normals}))

(defn refresh-manhattan-layout [app-state node]
  (let [source-node (first (e/get-related-nodes app-state node :start))
        target-node (first (e/get-related-nodes app-state node :end))
        update (cond
                 (and (some? source-node) (nil? target-node))
                 (manhattan-data (-> (nearest-control source-node (std/get-relation-end node))
                                     (center-point))
                                 (std/get-relation-end node))
                 (and (some? target-node) (nil? source-node))
                 (manhattan-data (std/get-relation-start node)
                                 (-> (nearest-control target-node (std/get-relation-start node))
                                     (center-point)))
                 (and (some? source-node) (some? target-node))
                 (let [{:keys [src trg]} (nearest-controls-between app-state source-node target-node)
                       vectors (calculate-vectors app-state source-node src target-node trg)]
                   {:tail (center-point src) :head (center-point trg) :vectors vectors}))]
    (update-manhattan-layout app-state node (:tail update) (:head update) (:vectors update))
    (std/align-decorators (e/node-by-id app-state (:uid node)))
    (std/calc-association-bbox app-state node)))

(defn set-relation-endpoints [app-state node tail-pos head-pos]
  (let [vectors (eval-vectors tail-pos head-pos)]
    (update-manhattan-layout app-state node tail-pos head-pos vectors)
    (std/align-decorators (e/node-by-id app-state (:uid node)))
    (std/calc-association-bbox app-state node)))
