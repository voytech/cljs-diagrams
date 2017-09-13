(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [core.behaviours :as cb]
            [impl.behaviours.standard :as std]
            [impl.drawables :as dimpl]))

;; component assertion may be some abstraction in behabviours.

(defn assert-component
  ([entity name type data]
   (js/console.log (clj->js data))
   (let [component (e/get-entity-component entity name)]
     (if (or (nil? component) (not= type (:type component)))
       (e/add-entity-component entity (e/new-component type name data))
       (d/set-data (:drawable component) data))))
  ([entity name type]
   (assert-component entity name type {})))

(defn assert-components [entity])

(defn- center-point [cmp]
  (let [d (:drawable cmp)
        mx (+ (d/get-left d) (/ (d/get-width d) 2))
        my (+ (d/get-top d) (/ (d/get-height d) 2))]
    {:x mx :y my}))

(defn- compute-mid-points [entity start end s-normal e-normal]
  (let [sp (center-point start)
        ep (center-point end)
        dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
     (cond
       (and (= (:h e-normal)) (= (:h s-normal))) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
       (and (= (:v e-normal)) (= (:v s-normal))) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
       (and (= (:v s-normal)) (= (:h e-normal))) [{:x (:x ep) :y (:y sp)}]
       (and (= (:h s-normal)) (= (:v e-normal))) [{:x (:x ep) :y (:y sp)}])))

(defn- compute-path [start-point end-point mid-points]
  (if (= 2 (count mid-points))
    [[start-point (first mid-points)] mid-points [(last mid-points) end-point]]
    [[start-point (first mid-points)] [(last mid-points) end-point]]))

(defn- update-line-component [entity idx sx sy ex ey]
  (assert-component entity (str "line-" idx) :relation {:x1 sx :y1 sy :x2 ex :y2 ey}))

(defn- update-line-components [entity path]
  (let [idx (volatile! 0)]
    (doseq [entry path]
     (update-line-component entity @idx (:x (first entry))
                                        (:y (first entry))
                                        (:x (peek entry))
                                        (:y (peek entry)))
     (vswap! idx inc))))

(defn update-manhattan-layout [entity s-normal e-normal]
  (let [start (e/get-entity-component entity "start")
        end (e/get-entity-component entity "end")
        connector (e/get-entity-component entity "connector")
        mid-points (compute-mid-points entity start end s-normal e-normal)
        path (compute-path (center-point start) (center-point end) mid-points)]
     (update-line-components entity path)))

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
        (cond
          (= :startpoint (:type endpoint)) (std/position-startpoint entity (:movement-x e) (:movement-y e) :offset)
          (= :endpoint   (:type endpoint)) (std/position-endpoint   entity (:movement-x e) (:movement-y e) :offset))
        (update-manhattan-layout entity (first normals) (last normals)))))