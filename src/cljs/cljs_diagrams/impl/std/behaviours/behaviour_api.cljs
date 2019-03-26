(ns cljs-diagrams.impl.std.behaviours.behaviour-api
  (:require [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.layouts :as layouts]
            [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.events :as ev]
            [cljs-diagrams.core.behaviours :as bhv]
            [cljs-diagrams.core.behaviour-api :as api]
            [cljs-diagrams.impl.std.shapes :as c]))

(defn calculate-angle [x1 y1 x2 y2]
   (let [PI 3.14
         x (- x2 x1)
         y (- y2 y1)
         angle (if (= x 0)
                   (if (= y 0)
                     0
                     (if (> y 0)
                       (/ PI 2)
                       (/ (* PI 3) 2)))
                   (if (= y 0)
                      (if (> x 0)
                          0
                          PI)
                      (if (< x 0)
                        (+ (js/Math.atan (/ y x)) PI)
                        (if (< y 0)
                          (+ (js/Math.atan (/ y x)) (* 2 PI))
                          (js/Math.atan (/ y x))))))]
       (/ (* angle 180) PI)))

(defn tail-vector [relation]
  (let [rel-points (d/getp relation :points)
        v (take 4 rel-points)]
    {:x1 (nth v 2) :y1 (nth v 3) :x2 (nth v 0) :y2 (nth v 1)}))

(defn head-vector [relation]
  (let [rel-points (d/getp relation :points)
        v (take-last 4 rel-points)]
    {:x1 (nth v 0) :y1 (nth v 1) :x2 (nth v 2) :y2 (nth v 3)}))

(defn get-relation-end [node]
  (when-let [relation (first (e/get-node-shape node ::c/relation))]
    (let [end (take-last 2 (d/getp relation :points))]
      {:x (first end) :y (last end)})))

(defn get-relation-start [node]
  (when-let [relation (first (e/get-node-shape node ::c/relation))]
    (let [end (take 2 (d/getp relation :points))]
      {:x (first end) :y (last end)})))

(defn nearest-control [app-state shape trg-node]
 (let [trg-connectors (e/get-node-shape trg-node ::c/control)]
     (->> (for [trg trg-connectors]
            {:src shape :trg trg :d (api/distance (api/center-point shape) (api/center-point trg))})
          (apply min-key #(:d %)))))

(defn snap-to-control [app-state shape trg-node]
  (let [{:keys [trg] :as n} (nearest-control app-state shape trg-node)
         trg-center (api/center-point trg)]
     (d/set-data shape {:left (- (:x trg-center) (/ (d/get-width shape) 2))
                        :top  (- (:y trg-center) (/ (d/get-height shape) 2))})))

(defn toggle-bbox [bbox toggle]
  (when (= ::c/bounding-box (:type bbox))
      (d/set-data bbox {:visible toggle})))

(defn toggle-controls [node toggle]
  (doseq [shape (e/shapes-of node)]
    (when (= ::c/control (:type shape))
       (d/set-data shape {:visible toggle :border-color "#ff0000"}))))

(defn toggle-control [control toggle]
  (when (= ::c/control (:type control))
      (d/set-data control {:visible toggle :border-color "#ff0000"})))

(defn resize-with-control [app-state node control movement-x movement-y]
  (let [node (e/node-by-id app-state (:uid node))]
    (when (= ::c/control (:type control))
      (let [side (e/shape-attribute app-state node (:name control) :side)
            bbox (:bbox node)]
        (->> (cond
               (= side :right)  (e/set-bbox app-state node (merge bbox {:width (+ (:width bbox) movement-x)}))
               (= side :bottom) (e/set-bbox app-state node (merge bbox {:height (+ (:height bbox) movement-y)})))
             (layouts/do-layouts app-state))))))
