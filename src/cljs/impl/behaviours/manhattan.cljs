(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [core.behaviours :as cb]
            [impl.drawables :as dimpl]))

;; component assertion may be some abstraction in behabviours.

(defn assert-component
  ([entity name type data]
   (let [component (e/get-entity-component entity name)]
     (when (or (nil? component)) (not= type (:type component))
       (e/add-entity-component entity (e/new-component type name data {})))))
  ([entity name type]
   (assert-component entity name type {})))

(defn assert-components [entity])

(defn- center-point [cmp]
  (let [drawable (:drawable cmp)
        mx (+ (d/get-left drawable) (/ (d/get-width drawable) 2))
        my (+ (d/get-top drawable) (/ (d/get-height drawable) 2))]
    {:x mx :y my}))

(defn- compute-mid-points [entity start end s-normal e-normal]
  (let [sp (center-point start)
        ep (center-point end)
        dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
    (let [mid-points (cond
                      (and (= (:h e-normal)) (= (:h s-normal))) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
                      (and (= (:v e-normal)) (= (:v s-normal))) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
                      (and (= (:v s-normal)) (= (:h e-normal))) [{:x (:x ep) :y (:y sp)}]
                      (and (= (:h s-normal)) (= (:v e-normal))) [{:x (:x ep) :y (:y sp)}])])))

(defn- compute-path [start-point end-point mid-points]
  (if (= 2 (count mid-points))
    [[start-point (first mid-points)] mid-points [(last mid-points end-point)]]
    [[start-point (first mid-points)] [(last mid-points end-point)]]))

(defn- update-line-component [entity idx sx sy ex ey]
  (let [line (assert-component entity (str "line-" idx) :relation {:x1 sx :y1 sy :x2 ex :y2 ey})]))


(defn- update-line-components [entity path]
  (map (fn [idx item] (update-line-components entity idx (first (first item))
                                                         (peek (first item))
                                                         (first (second item))
                                                         (peek (second item)))) path))

(defn initialise-manhattan-layout [entity s-normal e-normal]
  (let [start (e/get-entity-component entity "start")
        end (e/get-entity-component entity "end")
        connector (e/get-entity-component entity "connector")
        mid-points (compute-mid-points start end s-normal e-normal)
        path (compute-path (center-point start) (center-point end) mid-points)]
     (update-line-components entity path)))

(defn update-manhattan-layout [entity s-normal e-normal]
  (let [start (e/get-entity-component entity "start")
        end (e/get-entity-component entity "end")
        connector (e/get-entity-component entity "connector")]))

(defn calculate-normals [entity startpoint endpoint]
  [:h :h])

(defn manhattan-layout-moving-behaviour []
  (fn [e]
     (let [endpoint (:component e)
           entity   (:entity e)
           drawable (:drawable endpoint)
           start (e/get-entity-component entity "start")
           end (e/get-entity-component entity "end")
           connector (e/get-entity-component entity "connector")
           normals (calculate-normals entity start end)]
        (initialise-manhattan-layout entity (first normals) (last normals))   
        (cond
          (= :startpoint (:type endpoint)) (position-startpoint entity (:movement-x e) (:movement-y e) :offset)
          (= :endpoint   (:type endpoint)) (position-endpoint   entity (:movement-x e) (:movement-y e) :offset))))  `)
