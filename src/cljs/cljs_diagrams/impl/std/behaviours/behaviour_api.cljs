(ns cljs-diagrams.impl.std.behaviours.behaviour-api
  (:require [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.layouts :as layouts]
            [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.events :as ev]
            [cljs-diagrams.core.behaviours :as bhv]
            [cljs-diagrams.core.behaviour-api :as api]
            [cljs-diagrams.impl.std.shapes :as c]))

(declare position-endpoint)
(declare position-startpoint)
(declare position-breakpoint)
(declare dissoc-breakpoint)

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

(defn refresh-decorator-angle [{:keys [x1 y1 x2 y2] :as heading-vector} decorator]
  (when (not (nil? heading-vector))
    (d/setp decorator :angle (calculate-angle x1 y1 x2 y2))))

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

(defn get-decorators [node type]
  (let [relation (first (e/get-node-shape node ::c/relation))
        names (get-in relation [:attributes :decorators type])]
   (mapv #(e/get-node-shape node %) names)))

(defn align-decorators [node]
  (let [head-decs (get-decorators node :head)
        tail-decs (get-decorators node :tail)
        start-pos (get-relation-start node)
        end-pos   (get-relation-end node)
        relation  (first (e/get-node-shape node ::c/relation))]
    (doseq [head head-decs]
      (d/set-data head {:left (- (:x end-pos) (/ (d/get-width head) 2))
                        :top (- (:y end-pos) (/ (d/get-height head) 2))})
      (refresh-decorator-angle (head-vector relation) head))
    (doseq [tail tail-decs]
      (d/set-data tail {:left (- (:x start-pos) (/ (d/get-width tail) 2))
                        :top (- (:y start-pos) (/ (d/get-height tail) 2))})
      (refresh-decorator-angle (tail-vector relation) tail))))

(defn position-startpoint
  ([app-state node left top coord-mode skip?]
   (let [startpoint-component (e/get-node-shape node "start")
         position (effective-position startpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-node-shape node (:start (:attributes startpoint-component)))
         arrow-component (e/get-node-shape node "arrow")]
     (d/set-data startpoint-component {:left effective-left :top  effective-top})
     (when (= false skip?)
      (api/to-the-center-of starts-relation-component :x1 :y1 startpoint-component)
      (when (= true (:penultimate (:attributes startpoint-component)))
        (refresh-decorator-angle starts-relation-component arrow-component)))))
  ([app-state node left top]
   (position-startpoint app-state node left top :absolute false)))

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

(defn position-endpoint
  ([app-state node left top coord-mode skip?]
   (let [endpoint-component  (e/get-node-shape node "end")
         position (effective-position endpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         ends-relation-component  (e/get-node-shape node (:end (:attributes endpoint-component)))

         arrow-component      (e/get-node-shape node "arrow")]
    (d/set-data endpoint-component {:left effective-left  :top  effective-top})
    (api/to-the-center-of arrow-component :left :top endpoint-component)
    (when (= false skip?)
     (api/to-the-center-of ends-relation-component :x2 :y2 endpoint-component)
     (refresh-decorator-angle ends-relation-component arrow-component))))
 ([app-state node left top]
  (position-endpoint app-state node left top :absolute false)))

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
        (-> (cond
              (= side :right)  (e/set-bbox app-state node (merge bbox {:width (+ (:width bbox) movement-x)}))
              (= side :bottom) (e/set-bbox app-state node (merge bbox {:height (+ (:height bbox) movement-y)})))
            (layouts/do-layouts))))))

(defn calc-association-bbox [app-state node]
  (let [node     (e/node-by-id app-state (:uid node))
        startpoint (first (e/get-node-shape node ::c/startpoint))
        endpoint   (first (e/get-node-shape node ::c/endpoint))
        shapes [startpoint endpoint]]
    (when (and (some? startpoint) (some? endpoint))
      (let [leftmost   (apply min-key (concat [#(d/get-left %)] shapes))
            rightmost  (apply max-key (concat [#(+ (d/get-left %) (d/get-width %))] shapes))
            topmost    (apply min-key (concat [#(d/get-top %)] shapes))
            bottommost (apply max-key (concat [#(+ (d/get-top %) (d/get-height %))] shapes))]
        (-> (e/set-bbox app-state
                        node
                       {:left   (d/get-left leftmost)
                        :top    (d/get-top topmost)
                        :width  (- (+ (d/get-left rightmost) (d/get-width rightmost)) (d/get-left leftmost))
                        :height (- (+ (d/get-top bottommost) (d/get-height bottommost)) (d/get-top topmost))})
            (layouts/do-layouts))))))

(defn moving-endpoint []
   (fn [e]
      (let [endpoint (:shape e)
            node   (:node e)
            app-state (-> e :app-state)]
         (cond
           (= ::c/breakpoint (:type endpoint)) (position-breakpoint app-state node (:name endpoint) (:movement-x e) (:movement-y e) :offset)
           (= ::c/startpoint (:type endpoint)) (position-startpoint app-state node (:movement-x e) (:movement-y e) :offset false)
           (= ::c/endpoint   (:type endpoint)) (position-endpoint   app-state node (:movement-x e) (:movement-y e) :offset false)))))
