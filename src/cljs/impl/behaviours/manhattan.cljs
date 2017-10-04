(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [impl.behaviours.standard-api :as std]
            [impl.drawables :as dimpl]
            [impl.components :as c]))

(defn- center-point [cmp]
  (let [drwbl (:drawable cmp)
        mx (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2))
        my (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2))]
    {:x mx :y my}))

(defn- compute-mid-points [sp ep s-normal e-normal]
  (let [dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
     (cond
       (and (= :h e-normal) (= :h s-normal)) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
       (and (= :v e-normal) (= :v s-normal)) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
       (and (= :v s-normal) (= :h e-normal)) [{:x (:x sp) :y (:y ep)}]
       (and (= :h s-normal) (= :v e-normal)) [{:x (:x ep) :y (:y sp)}])))

(defn- compute-node-points [node-entity inset-width]
  (let [main (first (e/get-entity-component node-entity ::c/main))
        drwbl (:drawable main)]
   {:lt {:x (- (d/get-left drwbl) inset-width) :y (- (d/get-top drwbl) inset-width)}
    :lm {:x (- (d/get-left drwbl) inset-width) :y (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2))}
    :lb {:x (- (d/get-left drwbl) inset-width) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width)}
    :rt {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (- (d/get-top drwbl) inset-width)}
    :rm {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2))}
    :rb {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width)}
    :tm {:x (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2)) :y (- (d/get-top drwbl) inset-width)}
    :bm {:x (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2)) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width)}}))


(defn- sercterc-src-above-trg [s-conn-side t-conn-side source-main-c target-main-c]
  (and (= :right s-conn-side)
       (= :right t-conn-side)
       (< (d/get-left (:drawable target-main-c)) (d/get-left (:drawable source-main-c)))
       (< (d/get-top (:drawable target-main-c)) (d/get-top (:drawable source-main-c)))))

(defn- compute-candidate-points [entity start end s-normal e-normal]
  (let [sp (center-point start)
        ep (center-point end)
        source-node-id (e/component-property entity (:name start) :rel-entity-uid)
        target-node-id (e/component-property entity (:name end) :rel-entity-uid)]
    (if-not (or (nil? source-node-id) (nil? target-node-id))
      (let [source-c-side (e/component-property entity (:name start) :rel-connector)
            target-c-side (e/component-property entity (:name end) :rel-connector)
            source-node (e/entity-by-id source-node-id)
            target-node (e/entity-by-id target-node-id)
            source-n-points (compute-node-points source-node 50)
            target-n-points (compute-node-points target-node 50)
            source-main-c (first (e/get-entity-component source-node ::c/main))
            target-main-c (first (e/get-entity-component target-node ::c/main))]
        (when-not (or (nil? source-node) (nil? target-node))
          (cond
            (sercterc-src-above-trg source-c-side target-c-side source-main-c target-main-c)
            (concat [sp (:rm source-n-points) (:rt source-n-points) (:lt source-n-points)] (compute-mid-points (:lt source-n-points) ep :h :h))
            :esle
            (compute-mid-points (center-point start) (center-point end) s-normal e-normal))))
      (compute-mid-points (center-point start) (center-point end) s-normal e-normal))))

(defn- compute-path [start-point end-point mid-points]
  (let [all-points (flatten [start-point mid-points end-point])]
     (partition 2 1 all-points)))

(defn- update-line-component [entity idx sx sy ex ey]
  (e/assert-component entity (str "line-" idx) ::c/relation {:x1 sx :y1 sy :x2 ex :y2 ey}))

(defn- update-line-components [entity path]
  (let [remove-components (filter (fn [c]
                                    (let [splt (clojure.string/split (:name c) #"-")]
                                      (when (> (count splt) 1)
                                        (>= (cljs.reader/read-string (splt 1)) (count path))))) (e/components-of entity))]
    (if (> (count remove-components) 0)
      (doseq [component remove-components] (e/remove-entity-component entity (:name component))))
    (-> (map-indexed (fn [idx e]
                         (update-line-component entity idx (:x (first e))
                                                           (:y (first e))
                                                           (:x (last e))
                                                           (:y (last e)))) path)
        last)))

(defn update-manhattan-layout [entity s-normal e-normal]
  (let [start (e/get-entity-component entity "start")
        end (e/get-entity-component entity "end")
        mid-points (compute-candidate-points entity start end s-normal e-normal)
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
