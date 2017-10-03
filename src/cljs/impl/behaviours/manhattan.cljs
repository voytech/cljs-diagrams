(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [impl.behaviours.standard-api :as std]
            [impl.drawables :as dimpl]
            [impl.components :as c]))

;; component assertion may be some abstraction in behabviours.

(defn- center-point [cmp]
  (let [d (:drawable cmp)
        mx (+ (d/get-left d) (/ (d/get-width d) 2))
        my (+ (d/get-top d) (/ (d/get-height d) 2))]
    {:x mx :y my}))

(defn- compute-node-points [node-entity])

(defn- compute-candidate-points [entity start end]
  (let [sp (center-point start)
        ep (center-point end)
        source-node-id (e/component-property entity (:name start) :rel-entity-uid)
        target-node-id (e/component-property entity (:name end) :rel-entity-uid)]
    (when-not (nil? source-node-id))

    (when-not (nil? target-node-id))))

(defn- compute-mid-points [entity start end s-normal e-normal]
  (let [sp (center-point start)
        ep (center-point end)
        dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
     (cond
       (and (= :h e-normal) (= :h s-normal)) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
       (and (= :v e-normal) (= :v s-normal)) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
       (and (= :v s-normal) (= :h e-normal)) [{:x (:x sp) :y (:y ep)}]
       (and (= :h s-normal) (= :v e-normal)) [{:x (:x ep) :y (:y sp)}])))

(defn- compute-path [start-point end-point mid-points]
  (if (= 2 (count mid-points))
    [[start-point (first mid-points)] mid-points [(last mid-points) end-point]]
    [[start-point (first mid-points)] [(last mid-points) end-point]]))

(defn- update-line-component [entity idx sx sy ex ey]
  (e/assert-component entity (str "line-" idx) ::c/relation {:x1 sx :y1 sy :x2 ex :y2 ey}))

(defn- update-line-components [entity path]
  (let [remove-components (filter (fn [c]
                                    (let [splt (clojure.string/split (:name c) #"-")]
                                      (when (> (count splt) 1)
                                        (>= (cljs.reader/read-string (splt 1)) (count path))))) (e/components-of entity))]
    (if (> (count remove-components) 0)
      (doseq [component remove-components] (e/remove-entity-component entity (:name component))))
    (-> (map-indexed (fn [idx e] (update-line-component entity idx (:x (first e))
                                                                   (:y (first e))
                                                                   (:x (peek e))
                                                                   (:y (peek e)))) path)
        last)))

(defn update-manhattan-layout [entity s-normal e-normal]
  (let [start (e/get-entity-component entity "start")
        end (e/get-entity-component entity "end")
        mid-points (compute-mid-points entity start end s-normal e-normal)
        path (compute-path (center-point start) (center-point end) mid-points)]
     (-> (update-line-components entity path)
         (std/refresh-arrow-angle (e/get-entity-component entity "arrow")))))

(defn calculate-normals [entity startpoint endpoint]
  (let [start-c-point (center-point startpoint)
        end-c-point (center-point endpoint)
        startpoint-connector (e/component-property entity (:name startpoint) :rel-connector)
        endpoint-connector (e/component-property entity (:name endpoint) :rel-connector)]
    (cond
       (and (= :right startpoint-connector)  (= :left endpoint-connector)) [:h :h]
       (and (= :top startpoint-connector)    (= :left endpoint-connector)) [:v :h]
       (and (= :bottom startpoint-connector) (= :left endpoint-connector)) [:v :h]
       :else (if (> (- (:x end-c-point) (:x start-c-point)) (- (:y end-c-point) (:y start-c-point))) [:h :h] [:v :v]))))

(defn clear-orphan-components [entity start end]
  (e/remove-entity-component entity "connector"))
  ;(if (-> start :props)))

(defn manhattan-layout-moving-behaviour []
  (fn [e]
     (let [endpoint (:component e)
           entity   (:entity e)
           drawable (:drawable endpoint)
           start (e/get-entity-component entity "start")
           end (e/get-entity-component entity "end")
           connector (e/get-entity-component entity "connector")
           normals (calculate-normals entity start end)]
        (clear-orphan-components entity start end)
        (cond
          (= ::c/startpoint (:type endpoint)) (std/position-startpoint entity (:movement-x e) (:movement-y e) :offset true)
          (= ::c/endpoint   (:type endpoint)) (std/position-endpoint   entity (:movement-x e) (:movement-y e) :offset true))
        (update-manhattan-layout entity (normals 0) (normals 1)))))
