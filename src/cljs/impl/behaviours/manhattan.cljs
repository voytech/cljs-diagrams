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

(defn- eval-vectors [start end]
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

(defn update-manhattan-layout [app-state entity start end s-normal e-normal]
  (let [points (find-path start end s-normal e-normal)
        polyline-points (to-polyline-points start end points)
        polyline (first (e/get-entity-component entity ::c/relation))]
     (d/setp polyline :points polyline-points)
     (std/refresh-arrow-angle
       (take-last 4 polyline-points)
       (e/get-entity-component entity "arrow"))))

(defn- calculate-vectors [app-state
                          source-entity source-control
                          target-entity target-control]
  (let [source-control-side (e/component-attribute app-state source-entity (:name source-control) :side)
        target-control-side (e/component-attribute app-state target-entity (:name target-control) :side)]
    [(if (or (= :left source-control-side) (= :right source-control-side)) :h :v)
     (if (or (= :left target-control-side) (= :right target-control-side)) :h :v)]))

(defn- nearest-controls-between [app-state src-entity trg-entity]
  (let [src-connectors (e/get-entity-component src-entity ::c/control)
        trg-connectors (e/get-entity-component trg-entity ::c/control)]
      (->> (for [src src-connectors
                 trg trg-connectors]
             {:src src :trg trg :d (distance (center-point src) (center-point trg))})
           (apply min-key #(:d %)))))

(defn- position-entity-endpoint
  ([app-state entity component movement-x movement-y]
   (api/apply-effective-position component movement-x movement-y :offset)
   (when (= (:type component) ::c/endpoint)
     (let [arrow (e/get-entity-component entity "arrow")]
       (api/apply-effective-position arrow movement-x movement-y :offset))))
  ([app-state entity endpoint to-point]
   (api/apply-effective-position endpoint (:x to-point) (:y to-point) :absolute)
   (when (= (:type endpoint) ::c/endpoint)
     (let [arrow (e/get-entity-component entity "arrow")
           x (+ (:x to-point) (/ (d/get-width arrow) 2))
           y (+ (:y to-point) (/ (d/get-height arrow) 2))]
       (api/apply-effective-position arrow x y :absolute)))))

(defn endpoint-move
  ([app-state entity end-type new-active-pos]
    (let [passive (e/get-entity-component entity (if (= :start end-type) "end" "start"))
          active (e/get-entity-component entity (if (= :start end-type) "start" "end"))
          passive-pos (center-point passive)
          vectors (eval-vectors passive-pos new-active-pos)]
      (position-entity-endpoint app-state entity active {:x (- (:x new-active-pos)
                                                               (/  (d/get-width active) 2))
                                                         :y (- (:y new-active-pos)
                                                               (/  (d/get-height active) 2))})
      (update-manhattan-layout app-state entity passive-pos new-active-pos (vectors 0) (vectors 1))
      (std/calc-association-bbox app-state entity)))
  ([app-state entity end-type movement-x movement-y]
      (let [passive (e/get-entity-component entity (if (= :start end-type) "end" "start"))
            active (e/get-entity-component entity (if (= :start end-type) "start" "end"))
            passive-pos (center-point passive)
            active-pos (center-point active)
            new-active-pos {:x (+ (:x active-pos) movement-x) :y (+ (:y active-pos) movement-y)}
            vectors (eval-vectors passive-pos new-active-pos)]
        (position-entity-endpoint app-state entity active movement-x movement-y)
        (update-manhattan-layout app-state entity passive-pos new-active-pos (vectors 0) (vectors 1))
        (std/calc-association-bbox app-state entity))))

(defn on-source-entity-event [event]
  (let [{:keys [app-state entity start end movement-x movement-y]} event]
    (if (or (nil? start) (nil? end))
      (let [related-entity (or start end)
            active (e/get-entity-component entity (if-not (nil? start) "start" "end"))
            passive (e/get-entity-component entity (if-not (nil? start) "end" "start"))
            vectors (eval-vectors (center-point active) (center-point passive))]
        (position-entity-endpoint app-state entity active movement-x movement-y)
        (update-manhattan-layout app-state entity (center-point active) (center-point passive) (vectors 0) (vectors 1)))
      (let [{:keys [src trg]} (nearest-controls-between app-state start end)
            startpoint (e/get-entity-component entity "start")
            endpoint (e/get-entity-component entity "end")
            vectors (calculate-vectors app-state start src end trg)]
        (position-entity-endpoint app-state entity startpoint {:x (d/get-left src) :y (d/get-top src)})
        (position-entity-endpoint app-state entity endpoint {:x (d/get-left trg) :y (d/get-top trg)})
        (update-manhattan-layout app-state entity (center-point src) (center-point trg) (vectors 0) (vectors 1))))
    (std/calc-association-bbox app-state entity)))
    ;(b/fire app-state "layouts.do" {:container (e/volatile-entity app-state entity)})))
