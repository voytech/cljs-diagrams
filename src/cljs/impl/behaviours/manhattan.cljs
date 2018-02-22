(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.components :as d]
            [core.eventbus :as b]
            [impl.behaviours.standard-api :as std]
            [impl.components :as c]))

(def INSET-WIDTH 20)
(defonce CONTROL_SUFFIX "-ctrl")
(defonce LINE_PREFIX "line-")

(defn config []
  {:inset-width INSET-WIDTH
   :path-editing true
   :points-editing true})

(def overrides (volatile! #{}))

(defn- center-point [cmp]
  (let [mx (+ (d/get-left cmp) (/ (d/get-width cmp) 2))
        my (+ (d/get-top cmp) (/ (d/get-height cmp) 2))]
    {:x mx :y my}))

(defn- control-side-2-node-point-type [control-side]
  (cond
    (= control-side :left)   :lm
    (= control-side :right)  :rm
    (= control-side :top)    :tm
    (= control-side :bottom) :bm))

(defn- eval-normals [start end]
   (if (> (- (:x end) (:x start)) (- (:y end) (:y start))) [:h :v] [:v :h]))

(defn- find-connection-points [sp ep s-normal e-normal]
  (let [dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
     (cond
       (and (= :h e-normal) (= :h s-normal)) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
       (and (= :v e-normal) (= :v s-normal)) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
       (and (= :v s-normal) (= :h e-normal)) [{:x (:x sp) :y (:y ep)}]
       (and (= :h s-normal) (= :v e-normal)) [{:x (:x ep) :y (:y sp)}])))

(defn- find-node-wrapping-points [node-entity-or-main-cmpnt inset-width]
  (let [main (if (instance? e/Entity node-entity-or-main-cmpnt)
               (first (e/get-entity-component node-entity-or-main-cmpnt ::c/main))
               node-entity-or-main-cmpnt)]
   (array-map :lt {:x (- (d/get-left main) inset-width) :y (- (d/get-top main) inset-width) :i 0}
              :tm {:x (+ (d/get-left main) (/ (d/get-width main) 2)) :y (- (d/get-top main) inset-width) :i 1}
              :rt {:x (+ (d/get-left main) (d/get-width main) inset-width) :y (- (d/get-top main) inset-width) :i 2}
              :rm {:x (+ (d/get-left main) (d/get-width main) inset-width) :y (+ (d/get-top main) (/ (d/get-height main) 2)) :i 3}
              :rb {:x (+ (d/get-left main) (d/get-width main) inset-width) :y (+ (d/get-top main) (d/get-height main) inset-width) :i 4}
              :bm {:x (+ (d/get-left main) (/ (d/get-width main) 2)) :y (+ (d/get-top main) (d/get-height main) inset-width) :i 5}
              :lb {:x (- (d/get-left main) inset-width) :y (+ (d/get-top main) (d/get-height main) inset-width) :i 6}
              :lm {:x (- (d/get-left main) inset-width) :y (+ (d/get-top main) (/ (d/get-height main) 2)) :i 7})))

(defn- distance [p1 p2]
  (js/Math.sqrt (+ (js/Math.pow (- (:x p2) (:x p1)) 2) (js/Math.pow (- (:y p2) (:y p1)) 2))))

(defn- nearest-point [node-points rel-point]
  (let [bag (vals node-points)]
   (apply min-key (fn [src-point] (distance src-point rel-point)) bag)))

(defn- dist-b-path [points local-src local-trg]
  (cond
    (> (:i local-src ) (:i local-trg))
    (vec (concat (subvec points (:i local-src) (count points)) (subvec points 0 (inc (:i local-trg)))))
    (> (:i local-trg) (:i local-src))
    (vec (rseq (vec (concat (subvec points (:i local-trg) (count points)) (subvec points 0 (inc (:i local-src)))))))
    :else
    [local-src]))

(defn- dist-a-path [points local-src local-trg]
  (cond
    (> (:i local-src ) (:i local-trg))
    (vec (rseq (subvec points (:i local-trg) (inc (:i local-src)))))
    (> (:i local-trg) (:i local-src))
    (subvec points (:i local-src) (inc (:i local-trg)))
    :else
    [local-src]))

(defn- shortest-path [points local-src local-trg]
  (let [distance (- (:i local-trg) (:i local-src))
        dist-a (js/Math.abs distance)
        dist-b (- (count points) dist-a)]
    (if (<= dist-a dist-b)
      (dist-a-path points local-src local-trg)
      (dist-b-path points local-src local-trg))))

(defn find-node-paths [source-node-main-cmpnt source-control-side target-node-main-cmpnt target-control-side]
  (let [src-node-points (find-node-wrapping-points source-node-main-cmpnt INSET-WIDTH)
        trg-node-points (find-node-wrapping-points target-node-main-cmpnt INSET-WIDTH)
        src-ctrl-point (get src-node-points (control-side-2-node-point-type source-control-side))
        trg-ctrl-point (get trg-node-points (control-side-2-node-point-type target-control-side))
        nearest-src-point (nearest-point src-node-points trg-ctrl-point)
        nearest-trg-point (nearest-point trg-node-points nearest-src-point)
        src-node-points-vec (vec (vals src-node-points))
        trg-node-points-vec (vec (vals trg-node-points))
        src-path (shortest-path src-node-points-vec src-ctrl-point nearest-src-point)
        trg-path (shortest-path trg-node-points-vec trg-ctrl-point nearest-trg-point)]
     {:src src-path
      :trg trg-path}))

(defn- find-axis
  ([points]
   (let [point (peek points)
         yaxis (filter #(= (:x point) (:x %)) points)
         xaxis (filter #(= (:y point) (:y %)) points)]
     (if (> (count yaxis) (count xaxis))
        {:points (vec yaxis) :axis :y}
        {:points (vec xaxis) :axis :x})))
  ([point-a point-b]
   (cond
     (= (:x point-a) (:x point-b)) :y
     (= (:y point-a) (:y point-b)) :x
     :else :xy)))

(defn- direction
  ([points]
   (when (> (count points) 1)
     (let [fst (peek (vec (drop-last points)))
           scn (peek points)]
       (direction fst scn))))
  ([point-a point-b]
   (+ (- (:x point-b) (:x point-a))
      (- (:y point-b) (:y point-a)))))

(defn- second-axis [axis]
  (if (= :x axis) :y :x))

(defn- cut-off [points cutoff on-axis compare]
  (let [c-axis (second-axis on-axis)]
    (vec (filter #(or (compare (on-axis %) (on-axis cutoff)) (not= (c-axis %) (c-axis cutoff))) points))))

(defn- flag-dissoc [path]
  (fn [idx item]
    (let [prev-item (if (> idx 0) (path (dec idx)))
          next-item (if (< idx (dec (count path))) (path (inc idx)))]
      (if (and (not (nil? prev-item)) (not (nil? next-item)))
        (assoc item :rm (= (find-axis prev-item item) (find-axis item next-item)))
        item))))

(defn- normalize-path [path]
  (let [vpath (vec path)]
    (vec (filter #(or (nil? (:rm %)) (= false (:rm %))) (map-indexed (flag-dissoc vpath) vpath)))))

(defn- normalize-path_ [path]
  (let [y-align (flatten (mapv (fn [e] [(e 0) (peek e)]) (filter (fn [e] (> (count e) 1)) (vals (group-by :x path)))))
        x-align (flatten (mapv (fn [e] [(e 0) (peek e)]) (filter (fn [e] (> (count e) 1)) (vals (group-by :y path)))))
        preserve (set (concat x-align y-align))]
    (vec (filter #(contains? preserve %) path))))

(defn- find-path [entity start end s-normal e-normal]
  (let [sp (center-point start)
        ep (center-point end)
        source-node-id (e/component-property entity (:name start) :rel-entity-uid)
        target-node-id (e/component-property entity (:name end) :rel-entity-uid)]
    (if-not (or (nil? source-node-id) (nil? target-node-id))
      (let [source-c-side (e/component-property entity (:name start) :rel-connector)
            target-c-side (e/component-property entity (:name end) :rel-connector)
            source-node (e/entity-by-id source-node-id)
            target-node (e/entity-by-id target-node-id)
            node-paths (find-node-paths source-node source-c-side target-node target-c-side)
            src-path-begin (:src node-paths)
            trg-path-begin (:trg node-paths)
            src-path-begin-point (peek src-path-begin)
            trg-path-begin-point (peek trg-path-begin)
            normals (eval-normals src-path-begin-point trg-path-begin-point)
            mid-points (find-connection-points src-path-begin-point trg-path-begin-point (normals 0) (normals 1))
            first-mid-point (mid-points 0)
            last-mid-point (peek mid-points)
            src-axis (find-axis src-path-begin)
            trg-axis (find-axis trg-path-begin)
            n-src-path-begin (cond
                               (and (< (direction (:points src-axis)) 0) (> (direction src-path-begin-point first-mid-point) 0))
                               (cut-off src-path-begin first-mid-point (:axis src-axis) >)
                               (and (> (direction (:points src-axis)) 0) (< (direction src-path-begin-point first-mid-point) 0))
                               (cut-off src-path-begin first-mid-point (:axis src-axis) <)
                               :else
                               src-path-begin)
            n-trg-path-begin (cond
                               (and (< (direction (:points trg-axis)) 0) (> (direction trg-path-begin-point last-mid-point) 0))
                               (cut-off trg-path-begin last-mid-point (:axis trg-axis) >)
                               (and (> (direction (:points trg-axis)) 0) (< (direction trg-path-begin-point last-mid-point) 0))
                               (cut-off trg-path-begin last-mid-point (:axis trg-axis) <)
                               :else
                               trg-path-begin)]
         (-> (concat n-src-path-begin mid-points (rseq n-trg-path-begin))
             normalize-path))
      (-> (find-connection-points (center-point start) (center-point end) s-normal e-normal)
          normalize-path))))

(defn- find-path-lines [start-point end-point mid-points]
  (let [all-points (flatten [start-point mid-points end-point])]
     (partition 2 1 all-points)))

(defn- connector-idx [name]
  (let [splt (clojure.string/split name #"-")]
    (if (> (count splt) 1) (cljs.reader/read-string (splt 1)) -1)))

(defn- point-match? [sx sy trg tx ty]
  (and (= sx  (d/getp trg tx))
       (= sy  (d/getp trg ty))))

(defn start-or-end-point? [x y target]
  (cond
    (point-match? x y target :x1 :y1) :start
    (point-match? x y target :x2 :y2) :end
    :else false))

(defn- get-line-transform-properties [entity line]
  (let [p1 {:x (d/getp line :x1) :y (d/getp line :y1)}
        p2 {:x (d/getp line :x2) :y (d/getp line :y2)}
        axis (find-axis p1 p2)]
    {:value (get p1 (second-axis axis)) :axis (second-axis axis)}))

(defn- match-ends [line1 line2]
  (cond-> []
    (and (= (d/getp line1 :x1) (d/getp line2 :x1))
         (= (d/getp line1 :y1) (d/getp line2 :y1)))
    (conj {:x1 :x1 :y1 :y1})
    (and (= (d/getp line1 :x1) (d/getp line2 :x2))
         (= (d/getp line1 :y1) (d/getp line2 :y2)))
    (conj {:x1 :x2 :y2 :y2})
    (and (= (d/getp line1 :x2) (d/getp line2 :x1))
         (= (d/getp line1 :y2) (d/getp line2 :y1)))
    (conj {:x2 :x1 :y2 :y1})
    (and (= (d/getp line1 :x2) (d/getp line2 :x2))
         (= (d/getp line1 :y2) (d/getp line2 :y2)))
    (conj {:x2 :x2 :y2 :y2})))

(defn- resolve-property-overrides
  ([state]
   {:override (if (= :x (:axis state)) {:x1 (:value state) :x2 (:value state)} {:y1 (:value state) :y2 (:value state)})})
  ([line related state]
   (let [match-ends (match-ends line related)
         props (if (= :x (:axis state)) #{:x1 :x2} #{:y1 :y2})]
     (if (= (count match-ends) 1)
       (let [match-end (match-ends 0)
             keys (clojure.set/intersection (set (keys match-end)) props)]
        {:override (into {} (mapv (fn [e] {(get match-end e) (d/getp line e)} ) keys))})
       {}))))

(defn- persist-overrides [entity line]
  (let [state (get-line-transform-properties entity line)
        prev-name (str "line-" (dec (connector-idx (:name line))))
        next-name (str "line-" (inc (connector-idx (:name line))))
        prev (e/get-entity-component entity prev-name)
        next (e/get-entity-component entity next-name)]
    (e/update-component-prop entity (:name line) :store-transform  (merge state (resolve-property-overrides state)))
    (when (not (nil? prev))
      (e/update-component-prop entity prev-name :store-transform  (merge state (resolve-property-overrides line prev state))))
    (when (not (nil? next))
      (e/update-component-prop entity next-name :store-transform  (merge state (resolve-property-overrides line next state))))
    (vswap! overrides conj (:name line))))

(defn- load-line-transform [entity line]
  (e/component-property entity line :store-transform))

(defn- clear-line-transform [entity line]
  (e/remove-component-prop entity line :store-transform))

(defn- set-editable [entity line]
  (let [x1 (d/getp line :x1)
        x2 (d/getp line :x2)
        y1 (d/getp line :y1)
        y2 (d/getp line :y2)
        axis (if (= x1 x2) :y :x)
        ctrl-name (str (:name line) "-ctrl")
        width (if (= :x axis) (/ (js/Math.abs (- x2 x1)) 2) 8)
        height (if (= :x axis) 8 (/ (js/Math.abs (- y2 y1)) 2))
        left  (if (= :x axis) (+ (if (<= x1 x2) x1 x2) (/ width 2)) (- x1 4))
        top   (if (= :x axis) (- y1 4) (+ (if (<= y1 y2) y1 y2) (/ height 2)))
        conn-idx (connector-idx (:name line))]
    (e/assert-component entity ctrl-name ::c/control {:left left :top top :width width :height height :visible false :border-color "green"})
    (e/update-component-prop entity ctrl-name :target-connector (:name line))
    (e/update-component-prop entity ctrl-name :prev-connector  (str "line-" (dec conn-idx)))
    (e/update-component-prop entity ctrl-name :next-connector  (str "line-" (inc conn-idx)))))

(defn- try-override-coords [entity name sx sy ex ey]
  (let [line (e/get-entity-component entity name)
        persisted-state (e/component-property entity name :store-transform)
        override (:override persisted-state)]
     {:x1 (or (:x1 override) sx)
      :y1 (or (:y1 override) sy)
      :x2 (or (:x2 override) ex)
      :y2 (or (:y2 override) ey)}))

(defn- update-line-component [entity idx sx sy ex ey]
  (let [name (str "line-" idx)
        data (try-override-coords entity name sx sy ex ey)
        line (e/assert-component entity name ::c/relation data)]
    (when (and (true? (:path-editing (config))) (> idx 0) (< idx (dec (count (e/get-entity-component entity ::c/relation)))))
       (set-editable entity line))
    line))

(defn- check-override [entity path]
  (let [vpath (vec path)]
    (when (> (count @overrides) 0)
      (doseq [name @overrides]
        (let [idx (connector-idx name)
              entry (vpath idx)
              persisted-state (load-line-transform entity name)
              axis (find-axis (first entry) (last entry))]
          (when (not (= (second-axis axis) (:axis persisted-state)))
            (e/remove-component-prop entity name :store-transform)
            (e/remove-component-prop entity (str "line-" (inc idx)) :store-transform)
            (e/remove-component-prop entity (str "line-" (dec idx)) :store-transform)
            (vreset! overrides (set (remove #(= name %) @overrides)))))))))

(defn- update-line-components [entity path]
  (check-override entity path)
  (e/remove-entity-components entity (fn [c] (>= (connector-idx (:name c)) (count path))))
  (-> (map-indexed (fn [idx e] (update-line-component (e/volatile-entity entity)  idx (:x (first e)) (:y (first e)) (:x (last e)) (:y (last e)))) path)
      last))

(defn update-manhattan-layout [entity s-normal e-normal]
  (let [start (e/get-entity-component entity "start")
        end (e/get-entity-component entity "end")
        points (find-path entity start end s-normal e-normal)
        path (find-path-lines (center-point start) (center-point end) points)]
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

;; Main behaviour function exposed by manhattan.
(defn do-manhattan-layout []
  (fn [e]
     (let [endpoint (:component e)
           entity   (:entity e)
           start (e/get-entity-component entity "start")
           end (e/get-entity-component entity "end")
           connector (e/get-entity-component entity "connector")
           normals (calculate-normals entity start end)]
        (e/remove-entity-component entity "connector")
        (cond
          (= ::c/startpoint (:type endpoint)) (std/position-startpoint entity (:movement-x e) (:movement-y e) :offset true)
          (= ::c/endpoint   (:type endpoint)) (std/position-endpoint   entity (:movement-x e) (:movement-y e) :offset true))
        (update-manhattan-layout entity (normals 0) (normals 1))
        (js/console.log (clj->js (e/volatile-entity entity)))
        (b/fire "layout.do" {:container (e/volatile-entity entity) :type :attributes}))))

(defn position-connector [entity connector m-x m-y]
  (when (not= m-x 0) (d/setp connector :x1 (+ (d/getp connector :x1) m-x)))
  (when (not= m-y 0) (d/setp connector :y1 (+ (d/getp connector :y1) m-y)))
  (when (not= m-x 0) (d/setp connector :x2 (+ (d/getp connector :x2) m-x)))
  (when (not= m-y 0) (d/setp connector :y2 (+ (d/getp connector :y2) m-y))))

(defn position-connector-end [connector xp yp m-x m-y]
  (d/setp connector xp (+ (d/getp connector xp) m-x))
  (d/setp connector yp (+ (d/getp connector yp) m-y)))

(defn control-connector []
  (fn [e]
    (let [movement-x (:movement-x e)
          movement-y (:movement-y e)
          control (:component e)
          entity   (:entity e)
          prev-conn-name (e/component-property entity (:name control) :prev-connector)
          trg-conn-name  (e/component-property entity  (:name control) :target-connector)
          next-conn-name (e/component-property entity (:name control) :next-connector)
          prev-conn   (e/get-entity-component entity prev-conn-name)
          trg-conn    (e/get-entity-component entity trg-conn-name)
          next-conn   (e/get-entity-component entity next-conn-name)
          axis (find-axis {:x (d/getp trg-conn :x1)
                           :y (d/getp trg-conn :y1)}
                          {:x (d/getp trg-conn :x2)
                           :y (d/getp trg-conn :y2)})
          constr-movement-x (if (= :x axis) 0 movement-x)
          constr-movement-y (if (= :x axis) movement-y 0)]
       (std/apply-effective-position control constr-movement-x constr-movement-y :offset)
       (when (not (nil? prev-conn))
          (cond
            (and (= (d/getp trg-conn :x1) (d/getp prev-conn :x1))
                 (= (d/getp trg-conn :y1) (d/getp prev-conn :y1)))
            (position-connector-end prev-conn :x1 :y1 constr-movement-x constr-movement-y)
            (and (= (d/getp trg-conn :x1) (d/getp prev-conn :x2))
                 (= (d/getp trg-conn :y1) (d/getp prev-conn :y2)))
            (position-connector-end prev-conn :x2 :y2 constr-movement-x constr-movement-y)
            (and (= (d/getp trg-conn :x2) (d/getp prev-conn :x1))
                 (= (d/getp trg-conn :y2) (d/getp prev-conn :y1)))
            (position-connector-end prev-conn :x1 :y1 constr-movement-x constr-movement-y)
            (and (= (d/getp trg-conn :x2) (d/getp prev-conn :x2))
                 (= (d/getp trg-conn :y2) (d/getp prev-conn :y2)))
            (position-connector-end prev-conn :x2 :y2 constr-movement-x constr-movement-y)))
       (when (not (nil? next-conn))
         (cond
           (and (= (d/getp trg-conn :x1) (d/getp next-conn :x1))
                (= (d/getp trg-conn :y1) (d/getp next-conn :y1)))
           (position-connector-end next-conn :x1 :y1 constr-movement-x constr-movement-y)
           (and (= (d/getp trg-conn :x1) (d/getp next-conn :x2))
                (= (d/getp trg-conn :y1) (d/getp next-conn :y2)))
           (position-connector-end next-conn :x2 :y2 constr-movement-x constr-movement-y)
           (and (= (d/getp trg-conn :x2) (d/getp next-conn :x1))
                (= (d/getp trg-conn :y2) (d/getp next-conn :y1)))
           (position-connector-end next-conn :x1 :y1 constr-movement-x constr-movement-y)
           (and (= (d/getp trg-conn :x2) (d/getp next-conn :x2))
                (= (d/getp trg-conn :y2) (d/getp next-conn :y2)))
           (position-connector-end next-conn :x2 :y2 constr-movement-x constr-movement-y)))
       (position-connector entity trg-conn constr-movement-x constr-movement-y)
       (persist-overrides entity trg-conn))))
