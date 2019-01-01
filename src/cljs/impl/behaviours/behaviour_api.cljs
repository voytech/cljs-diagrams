(ns impl.behaviours.behaviour-api
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.components :as d]
            [core.eventbus :as b]
            [core.events :as ev]
            [core.behaviours :as bhv]
            [core.behaviour-api :as api]
            [impl.components :as c]))

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
      (+ (/ (* angle 180) PI) 90)))

(defn refresh-arrow-angle [relation-component arrow-component]
  (when (not (nil? relation-component))
    (let [x1 (-> relation-component  (d/getp :x1))
          y1 (-> relation-component  (d/getp :y1))
          x2 (-> relation-component  (d/getp :x2))
          y2 (-> relation-component  (d/getp :y2))]
       (d/setp arrow-component :angle (calculate-angle x1 y1 x2 y2)))))

(defn insert-breakpoint []
  (fn [e]
      (let [entity (:entity e)
            line (:component e)
            app-state (-> e :app-state)
            line-start-breakpoint (e/get-entity-component app-state entity (:start (:attributes line)))
            line-end-breakpoint   (e/get-entity-component app-state entity (:end (:attributes line)))
            oeX  (d/getp line :x2)
            oeY  (d/getp line :y2)
            eX   (:left e)
            eY   (:top e)]
        (when (= ::c/relation (:type line))
          (d/set-data line {:x2 eX :y2 eY})
          (let [relation-id   (str (random-uuid))
                breakpoint-id (str (random-uuid))
                is-penultimate (= true (:penultimate (:attributes line-start-breakpoint)))]
            (-> entity
              (e/add-entity-component app-state ::c/relation relation-id  {:x1 eX :y1 eY :x2 oeX :y2 oeY} {:start breakpoint-id :end (:name line-end-breakpoint)})
              (e/add-entity-component app-state ::c/breakpoint breakpoint-id {:point [eX eY]} {:end (:name line) :start relation-id :penultimate is-penultimate}))
            (e/update-component-attribute app-state entity (:name line) :end breakpoint-id)
            (e/update-component-attribute app-state entity (:name line-end-breakpoint) :end relation-id)
            (when (= true is-penultimate)
              (e/update-component-attribute app-state entity (:name line-start-breakpoint) :penultimate false)))))))

(defn dissoc-breakpoint []
  (fn [e]
    (let [entity     (:entity e)
          breakpoint (:component e)
          app-state (-> e :app-state)
          line-end   (e/get-entity-component app-state entity (:start  (:attributes breakpoint)))
          line-endpoint (e/get-entity-component app-state entity (:end (:attributes line-end)))
          line-start (e/get-entity-component app-state entity (:end   (:attributes breakpoint)))
          line-startpoint (e/get-entity-component app-state entity (:start (:attributes line-start)))
          is-penultimate? (:penultimate (:attributes breakpoint))]
       (e/remove-entity-component app-state entity (:name breakpoint))
       (e/remove-entity-component app-state entity (:name line-end))
       (e/update-component-attribute app-state entity (:name line-start) :end (:name line-endpoint))
       (e/update-component-attribute app-state entity (:name line-endpoint) :end (:name line-start))
       (e/update-component-attribute app-state entity (:name line-startpoint) :penultimate is-penultimate?)
       (d/set-data drawable {:x2 (+ (d/getp line-endpoint :left) (/ (d/getp line-endpoint :width) 2))
                             :y2 (+ (d/getp line-endpoint :top) (/ (d/getp line-endpoint :height) 2))}))))

(defn position-breakpoint
  ([app-state entity name left top coord-mode]
   (let [breakpoint-component (e/get-entity-component app-state entity name)
         position (effective-position app-state breakpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component app-state entity (:start (:attributes breakpoint-component)))
         ends-relation-component (e/get-entity-component app-state entity (:end (:attributes breakpoint-component)))
         arrow-component (e/get-entity-component app-state entity "arrow")]
     (d/set-data  breakpoint-component {:left effective-left :top  effective-top})
     (api/to-the-center-of starts-relation-component  :x1 :y1 breakpoint-component)
     (api/to-the-center-of ends-relation-component :x2 :y2 breakpoint-component)
     (when (= true (:penultimate (:attributes breakpoint-component)))
       (refresh-arrow-angle starts-relation-component arrow-component))))
  ([app-state entity name left top]
   (position-breakpoint app-state entity name left top :absolute)))

(defn position-startpoint
  ([app-state entity left top coord-mode skip?]
   (let [startpoint-component (e/get-entity-component app-state entity "start")
         position (effective-position startpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-component (e/get-entity-component app-state entity (:start (:attributes startpoint-component)))
         arrow-component (e/get-entity-component app-state entity "arrow")]
     (d/set-data startpoint-component {:left effective-left :top  effective-top})
     (when (= false skip?)
      (api/to-the-center-of starts-relation-component :x1 :y1 startpoint-component)
      (when (= true (:penultimate (:attributes startpoint-component)))
        (refresh-arrow-angle starts-relation-component arrow-component)))))
  ([app-state entity left top]
   (position-startpoint app-state entity left top :absolute false)))

(defn nearest-control [app-state component trg-entity]
 (let [trg-connectors (e/get-entity-component app-state trg-entity ::c/control)]
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
   (let [endpoint-component  (e/get-entity-component app-state entity "end")
         position (effective-position endpoint-component left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         ends-relation-component  (e/get-entity-component app-state entity (:end (:attributes endpoint-component)))

         arrow-component      (e/get-entity-component app-state entity "arrow")]
    (d/set-data endpoint-component {:left effective-left  :top  effective-top})
    (api/to-the-center-of arrow-component :left :top endpoint-component)
    (when (= false skip?)
     (api/to-the-center-of ends-relation-component :x2 :y2 endpoint-component)
     (refresh-arrow-angle ends-relation-component arrow-component))))
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
  (let [startpoint (first (e/get-entity-component app-state entity ::c/startpoint))
        endpoint   (first (e/get-entity-component app-state entity ::c/endpoint))
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
