(ns cljs-diagrams.impl.std.behaviours.behaviour-api
  (:require [cljs-diagrams.core.entities :as e]
            [cljs-diagrams.core.layouts :as layouts]
            [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.events :as ev]
            [cljs-diagrams.core.behaviours :as bhv]
            [cljs-diagrams.core.behaviour-api :as api]
            [cljs-diagrams.impl.std.components :as c]))

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

(defn get-relation-end [entity]
  (when-let [relation (first (e/get-entity-component entity ::c/relation))]
    (let [end (take-last 2 (d/getp relation :points))]
      {:x (first end) :y (last end)})))

(defn get-relation-start [entity]
  (when-let [relation (first (e/get-entity-component entity ::c/relation))]
    (let [end (take 2 (d/getp relation :points))]
      {:x (first end) :y (last end)})))

(defn get-decorators [entity type]
  (let [relation (first (e/get-entity-component entity ::c/relation))
        names (get-in relation [:attributes :decorators type])]
   (mapv #(e/get-entity-component entity %) names)))

(defn align-decorators [entity]
  (let [head-decs (get-decorators entity :head)
        tail-decs (get-decorators entity :tail)
        start-pos (get-relation-start entity)
        end-pos   (get-relation-end entity)
        relation  (first (e/get-entity-component entity ::c/relation))]
    (doseq [head head-decs]
      (d/set-data head {:left (- (:x end-pos) (/ (d/get-width head) 2))
                        :top (- (:y end-pos) (/ (d/get-height head) 2))})
      (refresh-decorator-angle (head-vector relation) head))
    (doseq [tail tail-decs]
      (d/set-data tail {:left (- (:x start-pos) (/ (d/get-width tail) 2))
                        :top (- (:y start-pos) (/ (d/get-height tail) 2))})
      (refresh-decorator-angle (tail-vector relation) tail))))

(defn position-startpoint
  ([app-state entity left top coord-mode skip?]
   (let [startpoint-component (e/get-entity-component entity "start")
         position (effective-position startpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component entity (:start (:attributes startpoint-component)))
         arrow-component (e/get-entity-component entity "arrow")]
     (d/set-data startpoint-component {:left effective-left :top  effective-top})
     (when (= false skip?)
      (api/to-the-center-of starts-relation-component :x1 :y1 startpoint-component)
      (when (= true (:penultimate (:attributes startpoint-component)))
        (refresh-decorator-angle starts-relation-component arrow-component)))))
  ([app-state entity left top]
   (position-startpoint app-state entity left top :absolute false)))

(defn nearest-control [app-state component trg-entity]
 (let [trg-connectors (e/get-entity-component trg-entity ::c/control)]
     (->> (for [trg trg-connectors]
            {:src component :trg trg :d (api/distance (api/center-point component) (api/center-point trg))})
          (apply min-key #(:d %)))))

(defn snap-to-control [app-state component trg-entity]
  (let [{:keys [trg] :as n} (nearest-control app-state component trg-entity)
         trg-center (api/center-point trg)]
     (d/set-data component {:left (- (:x trg-center) (/ (d/get-width component) 2))
                            :top  (- (:y trg-center) (/ (d/get-height component) 2))})))

(defn position-endpoint
  ([app-state entity left top coord-mode skip?]
   (let [endpoint-component  (e/get-entity-component entity "end")
         position (effective-position endpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         ends-relation-component  (e/get-entity-component entity (:end (:attributes endpoint-component)))

         arrow-component      (e/get-entity-component entity "arrow")]
    (d/set-data endpoint-component {:left effective-left  :top  effective-top})
    (api/to-the-center-of arrow-component :left :top endpoint-component)
    (when (= false skip?)
     (api/to-the-center-of ends-relation-component :x2 :y2 endpoint-component)
     (refresh-decorator-angle ends-relation-component arrow-component))))
 ([app-state entity left top]
  (position-endpoint app-state entity left top :absolute false)))

(defn toggle-bbox [bbox toggle]
  (when (= ::c/bounding-box (:type bbox))
      (d/set-data bbox {:visible toggle})))

(defn toggle-controls [entity toggle]
  (doseq [component (e/components-of entity)]
    (when (= ::c/control (:type component))
       (d/set-data component {:visible toggle :border-color "#ff0000"}))))

(defn toggle-control [control toggle]
  (when (= ::c/control (:type control))
      (d/set-data control {:visible toggle :border-color "#ff0000"})))

(defn resize-with-control [app-state entity control movement-x movement-y]
  (let [entity (e/entity-by-id app-state (:uid entity))]
    (when (= ::c/control (:type control))
      (let [side (e/component-attribute app-state entity (:name control) :side)
            bbox (:bbox entity)]
        (-> (cond
              (= side :right)  (e/set-bbox app-state entity (merge bbox {:width (+ (:width bbox) movement-x)}))
              (= side :bottom) (e/set-bbox app-state entity (merge bbox {:height (+ (:height bbox) movement-y)})))
            (layouts/do-layouts))))))

(defn calc-association-bbox [app-state entity]
  (let [entity     (e/entity-by-id app-state (:uid entity))
        startpoint (first (e/get-entity-component entity ::c/startpoint))
        endpoint   (first (e/get-entity-component entity ::c/endpoint))
        components [startpoint endpoint]]
    (when (and (some? startpoint) (some? endpoint))
      (let [leftmost   (apply min-key (concat [#(d/get-left %)] components))
            rightmost  (apply max-key (concat [#(+ (d/get-left %) (d/get-width %))] components))
            topmost    (apply min-key (concat [#(d/get-top %)] components))
            bottommost (apply max-key (concat [#(+ (d/get-top %) (d/get-height %))] components))]
        (-> (e/set-bbox app-state
                        entity
                     {:left   (d/get-left leftmost)
                      :top    (d/get-top topmost)
                      :width  (- (+ (d/get-left rightmost) (d/get-width rightmost)) (d/get-left leftmost))
                      :height (- (+ (d/get-top bottommost) (d/get-height bottommost)) (d/get-top topmost))})
            (layouts/do-layouts))))))

(defn moving-endpoint []
   (fn [e]
      (let [endpoint (:component e)
            entity   (:entity e)
            app-state (-> e :app-state)]
         (cond
           (= ::c/breakpoint (:type endpoint)) (position-breakpoint app-state entity (:name endpoint) (:movement-x e) (:movement-y e) :offset)
           (= ::c/startpoint (:type endpoint)) (position-startpoint app-state entity (:movement-x e) (:movement-y e) :offset false)
           (= ::c/endpoint   (:type endpoint)) (position-endpoint   app-state entity (:movement-x e) (:movement-y e) :offset false)))))
