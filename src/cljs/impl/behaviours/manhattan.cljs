(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.components :as d]
            [core.eventbus :as b]
            [core.behaviour-api :as api]
            [impl.behaviours.behaviour-api :as std]
            [impl.components :as c]))


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

(defn update-manhattan-layout [app-state entity start end normals]
  (let [points (find-path start end (normals 0) (normals 1))
        polyline-points (to-polyline-points start end points)
        polyline (first (e/get-entity-component entity ::c/relation))]
     (d/setp polyline :points polyline-points)))

(defn- calculate-vectors [app-state
                          source-entity source-control
                          target-entity target-control]
  (let [source-control-side (e/component-attribute app-state source-entity (:name source-control) :side)
        target-control-side (e/component-attribute app-state target-entity (:name target-control) :side)]
    [(if (or (= :left source-control-side) (= :right source-control-side)) :h :v)
     (if (or (= :left target-control-side) (= :right target-control-side)) :h :v)]))

(defn nearest-control [entity point]
  (let [src-connectors (e/get-entity-component entity ::c/control)]
    (apply min-key #(distance (center-point %) point) src-connectors)))

(defn- nearest-controls-between [app-state src-entity trg-entity]
  (let [src-connectors (e/get-entity-component src-entity ::c/control)
        trg-connectors (e/get-entity-component trg-entity ::c/control)]
      (->> (for [src src-connectors
                 trg trg-connectors]
             {:src src :trg trg :d (distance (center-point src) (center-point trg))})
           (apply min-key #(:d %)))))

(defn move-point [pos movement-x movement-y]
  {:x (+ (:x pos) movement-x) :y (+ (:y pos) movement-y)})

(defn endpoint-move
  ([app-state entity end-type new-active-pos]
   (let [entity (e/entity-by-id app-state (:uid entity))
         tail-pos (if (= :start end-type)
                     new-active-pos
                     (std/get-relation-start entity))
         head-pos (if (= :start end-type)
                     (std/get-relation-end entity)
                     new-active-pos)
         vectors (eval-vectors tail-pos head-pos)]
     (update-manhattan-layout app-state entity tail-pos head-pos vectors)
     (std/align-decorators (e/entity-by-id app-state (:uid entity)))
     (std/calc-association-bbox app-state entity)))
  ([app-state entity end-type movement-x movement-y]
   (let [entity (e/entity-by-id app-state (:uid entity))
         tail-pos (if (= :start end-type)
                     (move-point (std/get-relation-start entity) movement-x movement-y)
                     (std/get-relation-start entity))
         head-pos (if (= :start end-type)
                     (std/get-relation-end entity)
                     (move-point (std/get-relation-end entity) movement-x movement-y))
         vectors (eval-vectors tail-pos head-pos)]
     (update-manhattan-layout app-state entity tail-pos head-pos vectors)
     (std/align-decorators (e/entity-by-id app-state (:uid entity)))
     (std/calc-association-bbox app-state entity))))

(defn manhattan-data [tail-pos head-pos]
  (let [normals  (eval-vectors tail-pos head-pos)]
    {:tail tail-pos :head head-pos :vectors normals}))

(defn refresh-manhattan-layout [app-state entity]
  (let [source-entity (first (e/get-related-entities app-state entity :start))
        target-entity (first (e/get-related-entities app-state entity :end))
        update (cond
                 (and (some? source-entity) (nil? target-entity))
                 (manhattan-data (-> (nearest-control source-entity (std/get-relation-end entity))
                                     (center-point))
                                 (std/get-relation-end entity))
                 (and (some? target-entity) (nil? source-entity))
                 (manhattan-data (std/get-relation-start entity)
                                 (-> (nearest-control target-entity (std/get-relation-start entity))
                                     (center-point)))                      
                 (and (some? source-entity) (some? target-entity))
                 (let [{:keys [src trg]} (nearest-controls-between app-state source-entity target-entity)
                       vectors (calculate-vectors app-state source-entity src target-entity trg)]
                   {:tail (center-point src) :head (center-point trg) :vectors vectors}))]
    (update-manhattan-layout app-state entity (:tail update) (:head update) (:vectors update))
    (std/align-decorators (e/entity-by-id app-state (:uid entity)))
    (std/calc-association-bbox app-state entity)))

(defn set-relation-endpoints [app-state entity tail-pos head-pos]
  (let [vectors (eval-vectors tail-pos head-pos)]
    (update-manhattan-layout app-state entity tail-pos head-pos vectors)
    (std/align-decorators (e/entity-by-id app-state (:uid entity)))
    (std/calc-association-bbox app-state entity)))
