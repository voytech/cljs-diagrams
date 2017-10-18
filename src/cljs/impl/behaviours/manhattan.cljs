(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [impl.behaviours.standard-api :as std]
            [impl.drawables :as dimpl]
            [impl.components :as c]))

(def INSET-WIDTH 20)

(defn- center-point [cmp]
  (let [drwbl (:drawable cmp)
        mx (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2))
        my (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2))]
    {:x mx :y my}))

(defn- control-side-2-node-point-type [control-side]
  (cond
    (= control-side :left)   :lm
    (= control-side :right)  :rm
    (= control-side :top)    :tm
    (= control-side :bottom) :bm))

(defn- eval-normals [start end]
   (if (> (- (:x end) (:x start)) (- (:y end) (:y start))) [:h :v] [:v :h]))

(defn- compute-mid-points [sp ep s-normal e-normal]
  (let [dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
     (cond
       (and (= :h e-normal) (= :h s-normal)) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
       (and (= :v e-normal) (= :v s-normal)) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
       (and (= :v s-normal) (= :h e-normal)) [{:x (:x sp) :y (:y ep)}]
       (and (= :h s-normal) (= :v e-normal)) [{:x (:x ep) :y (:y sp)}])))

(defn- compute-node-points [node-entity-or-main-cmpnt inset-width]
  (let [main (if (instance? e/Entity node-entity-or-main-cmpnt)
               (first (e/get-entity-component node-entity-or-main-cmpnt ::c/main))
               node-entity-or-main-cmpnt)
        drwbl (:drawable main)]
   (array-map :lt {:x (- (d/get-left drwbl) inset-width) :y (- (d/get-top drwbl) inset-width) :i 0}
              :tm {:x (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2)) :y (- (d/get-top drwbl) inset-width) :i 1}
              :rt {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (- (d/get-top drwbl) inset-width) :i 2}
              :rm {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2)) :i 3}
              :rb {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width) :i 4}
              :bm {:x (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2)) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width) :i 5}
              :lb {:x (- (d/get-left drwbl) inset-width) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width) :i 6}
              :lm {:x (- (d/get-left drwbl) inset-width) :y (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2)) :i 7})))

(defn- distance [p1 p2]
  (js/Math.sqrt (+ (js/Math.pow (- (:x p2) (:x p1)) 2) (js/Math.pow (- (:y p2) (:y p1)) 2))))

(defn- nearest-point [node-points rel-point]
  (let [bag (vals node-points)]
   (apply min-key (fn [src-point] (distance src-point rel-point)) bag)))

(defn- shortest-path [points local-src local-trg]
  (let [distance (- (:i local-trg) (:i local-src))
        dist-a (js/Math.abs distance)
        dist-b (- (count points) dist-a)]
    (if (< dist-a dist-b)
      (if (> distance 0)
        (subvec points (:i local-src) (inc (:i local-trg)))
        (vec (rseq (subvec points (:i local-trg) (inc (:i local-src))))))
      (let [path-b (concat (subvec points (:i local-src) (count points)) (subvec points 0 (inc (:i local-trg))))]
        (if (> distance 0)
          (vec (rseq (vec path-b)))
          (vec path-b))))))

(defn node-path-begining [source-node-main-cmpnt source-control-side target-node-main-cmpnt target-control-side]
  (let [src-node-points (compute-node-points source-node-main-cmpnt INSET-WIDTH)
        trg-node-points (compute-node-points target-node-main-cmpnt INSET-WIDTH)
        src-ctrl-point (get src-node-points (control-side-2-node-point-type source-control-side))
        trg-ctrl-point (get trg-node-points (control-side-2-node-point-type target-control-side))
        nearest-src-point (nearest-point src-node-points trg-ctrl-point)
        nearest-trg-point (nearest-point trg-node-points src-ctrl-point)
        src-node-points-vec (vec (vals src-node-points))
        trg-node-points-vec (vec (vals trg-node-points))
        src-path (shortest-path src-node-points-vec src-ctrl-point nearest-src-point)
        trg-path (shortest-path trg-node-points-vec trg-ctrl-point nearest-trg-point)]
     {:src src-path
      :trg trg-path}))



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
            path-beginings (node-path-begining source-node source-c-side target-node target-c-side)
            src-path-begin (:src path-beginings)
            trg-path-begin (:trg path-beginings)
            src-path-begin-point (peek src-path-begin)
            trg-path-begin-point (peek trg-path-begin)
            normals (eval-normals src-path-begin-point trg-path-begin-point)
            mid-points (compute-mid-points src-path-begin-point trg-path-begin-point (normals 0) (normals 1))
            first-mid-point (mid-points 0)
            last-mid-point (peek mid-points)
            src-outgoing-axis (cond
                                (= (:x src-path-begin-point) (:x first-mid-point) (:x (last (drop-last src-path-begin))))
                                :v
                                (= (:y src-path-begin-point) (:y first-mid-point) (:y (last (drop-last src-path-begin))))
                                :h
                                :else
                                false)
            trg-outgoing-axis (cond
                                (= (:x trg-path-begin-point) (:x last-mid-point) (:x (last (drop-last trg-path-begin))))
                                :v
                                (= (:y trg-path-begin-point) (:y last-mid-point) (:y (last (drop-last trg-path-begin))))
                                :h
                                :else
                                false)
            src-path-normalised (if src-outgoing-axis
                                    (vec (take-while #(or (< (:x first-mid-point) (:x %)) (< (:y first-mid-point) (:y %))) src-path-begin))
                                    src-path-begin)
            trg-path-normalised (if trg-outgoing-axis
                                    (vec (take-while #(or (< (:x last-mid-point) (:x %)) (< (:y last-mid-point) (:y %))) trg-path-begin))
                                    trg-path-begin)]
        (concat src-path-normalised mid-points (rseq trg-path-normalised)))
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
       (and (= :right startpoint-connector)  (= :left endpoint-connector)) [:v :h]
       (and (= :top startpoint-connector)    (= :left endpoint-connector)) [:v :h]
       (and (= :bottom startpoint-connector) (= :left endpoint-connector)) [:v :h]
       :else (if (> (- (:x end-c-point) (:x start-c-point)) (- (:y end-c-point) (:y start-c-point))) [:h :h] [:v :v]))))

(defn clear-orphan-components [entity start end]
  (e/remove-entity-component entity "connector"))

(defn do-manhattan-layout []
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
