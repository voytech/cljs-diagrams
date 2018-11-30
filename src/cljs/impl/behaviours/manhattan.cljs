(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.components :as d]
            [core.eventbus :as b]
            [impl.behaviours.standard-api :as std]
            [impl.components :as c]))

(defonce CONTROL_SUFFIX "-ctrl")
(defonce LINE_PREFIX "line-")

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
   (find-connection-points (center-point start) (center-point end) s-normal e-normal))

(defn- find-path-lines [start-point end-point mid-points]
  (let [all-points (flatten [start-point mid-points end-point])]
     (partition 2 1 all-points)))

(defn- connector-idx [name]
  (let [splt (clojure.string/split name #"-")]
    (if (> (count splt) 1) (cljs.reader/read-string (splt 1)) -1)))

(defn- update-line-component [entities entity idx sx sy ex ey]
  (let [name (str "line-" idx)]
    (e/assert-component c/relation entities entity name {:x1 sx :y1 sy :x2 ex :y2 ey})))

(defn- update-line-components [entities entity path]
  (e/remove-entity-components entities entity (fn [c] (>= (connector-idx (:name c)) (count path))))
  (-> (map-indexed (fn [idx e]
                      (update-line-component entities
                                             (e/volatile-entity entities entity)
                                             idx
                                             (:x (first e))
                                             (:y (first e))
                                             (:x (last e))
                                             (:y (last e))))
                   path)
      last))

(defn update-manhattan-layout [entities entity start end s-normal e-normal]
  (let [points (find-path start end s-normal e-normal)
        path (find-path-lines (center-point start) (center-point end) points)
        cstart (e/get-entity-component entities entity "start")
        cend (e/get-entity-component entities entity "end")]
     (-> (update-line-components entities entity path)
         (std/refresh-arrow-angle (e/get-entity-component entities entity "arrow")))))

(defn- calculate-vectors [entities
                          source-entity source-control
                          target-entity target-control]
  (let [source-control-side (e/component-property entities source-entity (:name source-control) :side)
        target-control-side (e/component-property entities target-entity (:name target-control) :side)]
    [(if (or (= :left source-control-side) (= :right source-control-side)) :h :v)
     (if (or (= :left target-control-side) (= :right target-control-side)) :h :v)]))

(defn- nearest-controls-between [entities src-entity trg-entity]
  (let [src-connectors (e/get-entity-component entities src-entity ::c/control)
        trg-connectors (e/get-entity-component entities trg-entity ::c/control)]
      (->> (for [src src-connectors
                 trg trg-connectors]
             {:src src :trg trg :d (distance (center-point src) (center-point trg))})
           (apply min-key #(:d %)))))

(defn- position-entity-endpoint
  ([entities entity component movement-x movement-y]
    (std/apply-effective-position component movement-x movement-y :offset)
    (when (= (:type component) ::c/endpoint)
      (let [arrow (e/get-entity-component entities entity "arrow")]
        (std/apply-effective-position arrow movement-x movement-y :offset))))
  ([entities entity endpoint to-point]
    (std/apply-effective-position endpoint (:x to-point) (:y to-point) :absolute)
    (when (= (:type endpoint) ::c/endpoint)
      (let [arrow (e/get-entity-component entities entity "arrow")
            x (+ (:x to-point) (/ (d/get-width arrow) 2))
            y (+ (:y to-point) (/ (d/get-height arrow) 2))]
        (std/apply-effective-position arrow x y :absolute)))))

(defn on-endpoint-event [event]
  (let [{:keys [app-state entity component movement-x movement-y]} event
        entities (-> app-state deref :entities)
        start (e/get-entity-component entities entity "start")
        end (e/get-entity-component entities entity "end")
        vectors (eval-vectors (center-point start) (center-point end))]
    (e/remove-entity-component entities entity "connector")
    (position-entity-endpoint entities entity component movement-x movement-y)
    (update-manhattan-layout entities entity start end (vectors 0) (vectors 1))
    (b/fire "layout.do" {:container (e/volatile-entity entities entity) :type :attributes})))

(defn on-source-entity-event [event]
  (let [{:keys [app-state entity start end movement-x movement-y]} event
        entities (-> app-state deref :entities)]
    (e/remove-entity-component entities entity "connector")
    (if (or (nil? start) (nil? end))
      (let [related-entity (or start end)
            active (e/get-entity-component entities entity (if-not (nil? start) "start" "end"))
            passive (e/get-entity-component entities entity (if-not (nil? start) "end" "start"))
            vectors (eval-vectors (center-point active) (center-point passive))]
        (position-entity-endpoint entities entity active movement-x movement-y)
        (update-manhattan-layout entities entity active passive (vectors 0) (vectors 1)))
      (let [{:keys [src trg]} (nearest-controls-between entities start end)
            startpoint (e/get-entity-component entities entity "start")
            endpoint (e/get-entity-component entities entity "end")
            vectors (calculate-vectors entities start src end trg)]
        (position-entity-endpoint entities entity startpoint {:x (d/get-left src) :y (d/get-top src)})
        (position-entity-endpoint entities entity endpoint {:x (d/get-left trg) :y (d/get-top trg)})
        (update-manhattan-layout entities entity src trg (vectors 0) (vectors 1) movement-x movement-y)))
    (b/fire "layout.do" {:container (e/volatile-entity entities entity) :type :attributes})))
