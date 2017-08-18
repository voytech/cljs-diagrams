(ns core.entities-behaviours
 (:require [core.entities :as e]
           [core.project :as p]
           [core.layouts :as layouts]
           [core.drawables :as d]
           [clojure.string :as str]))


(declare position-endpoint)
(declare position-startpoint)
(declare position-breakpoint)
(declare moving-endpoint)
(declare relation-line)
(declare endpoint)
(declare calculate-angle)
(declare dissoc-breakpoint)

(def DEFAULT_SIZE_OPTS {:width 180 :height 150})
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :background-color "#fff" :border-color "black" :border-width 1.5})
(def HANDLER_SMALLEST {:radius 8 :background-color "#fff" :border-color "black" :border-width 1.5})

(defn overlaying? [src trg]
  (intersercts? src trg))

(defn intersects-any? [components yes]
  (fn [e]
    (let [src    (:drawable e)
          entity (:entity e)
          part   (:component e)
          canvas (:canvas e)]
      (when (contains? #{"end" "start"} component)
          (.forEachObject canvas
                          #(when (and (not (== % src)) (contains? parts (.-refPartId %)))
                             (let [trg %
                                   src-ent (e/entity-from-src src)
                                   trg-ent (e/entity-from-src trg)
                                   src-part (.-refPartId src)
                                   trg-part (.-refPartId trg)]
                               (when (overlaying? src %)
                                 (yes {:src src :drawable src-part :entity src-ent} {:src trg :drawable trg-part :entity trg-ent})))))))))

(defn intersects? [part_ yes no]
  (fn [e]
    (let [src    (:src e)
          entity (:entity e)
          part   (:drawable e)
          canvas (:canvas e)]
      (when (contains? #{"end" "start"} part)
        (.forEachObject canvas
                        #(when (and (not (== % src)) (= part_ (.-refPartId %)))
                           (let [trg %
                                 src-ent (e/entity-from-src src)
                                 trg-ent (e/entity-from-src trg)
                                 src-part (.-refPartId src)
                                 trg-part (.-refPartId trg)]
                              (if (overlaying? src %)
                                (yes {:src src :drawable src-part :entity src-ent} {:src trg :drawable trg-part :entity trg-ent})
                                (no  {:src src :drawable src-part :entity src-ent} {:src trg :drawable trg-part :entity trg-ent})))))))))

(defn- assert-position-context [context]
  (when-not (or (= context :entity-scope)
                (= context :relation-scope)
                (= context :any-scope))
     (Error. "context must be entity-relation or component-relation")))

(defn- calculate-offset [component left top]
  {:left (- left (d/getp (:drawable component) :left))
   :top  (- top  (d/getp (:drawable component) :top))})

(defn- calculate-effective-offset [entity component-name left top coord-mode]
  (if (= :offset coord-mode)
    {:left left :top top}
    (let [component (e/get-entity-component entity component-name)]
      (calculate-offset component left top))))

(defn- effective-position
 ([component get-x get-y x y coord-mode]
  (let [effective-x (if (= :offset coord-mode) (+ (get-x (:drawable component)) x) x)
        effective-y  (if (= :offset coord-mode) (+ (get-y (:drawable component)) y) y)]
    {:x effective-x :y effective-y}))
 ([component x y coord-mode]
  (effective-position component #(d/getp % :left) #(d/getp % :top) x y coord-mode)))

(defn- apply-effective-position
 ([component set-x get-x set-y get-y x y coord-mode]
  (let [epos (effective-position component get-x get-y x y coord-mode)]
    (set-x (:drawable component) (:x epos))
    (set-y (:drawable component) (:y epos))))
 ([comopnent x y coord-mode]
  (apply-effective-position comopnent
                            #(d/setp %1 :left %2)
                            #(d/getp % :left)
                            #(d/setp %1 :top %2)
                            #(d/getp % :top)
                            x
                            y
                            coord-mode)))

(defn- position-attributes-components [attributes offset-left offset-top]
   (doseq [src (flatten (mapv #(e/components %) attributes))]
     (d/set-data (:drawable src) {:left (+ (d/getp (:drawable src) :left) offset-left)
                                  :top  (+ (d/getp (:drawable src) :top) offset-top)})))

(defmulti get-refered-component-name (fn [relation] (:type (e/entity-by-id (:entity-id relation)))))

(defmulti position-entity-component (fn [entity component-name context left top coord-mode]
                                     (assert-position-context context)
                                     [(:type entity) (:type (e/get-entity-component entity component-name)) context]))

(defmulti position-entity (fn [entity ref-component-name context left top coord-mode]
                            (assert-position-context context)
                            [(:type entity) context]))

;; ==============================================================================================
;; Multimethods for RECTANGLE-NODE entity positioning
;; ==============================================================================================

(defmethod get-refered-component-name "rectangle-node" [relation]
   "body")

(defmethod position-entity-component [ "rectangle-node" :main :entity-scope] [entity component-name context left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defmethod position-entity-component [ "rectangle-node" :endpoint :entity-scope] [entity component-name context left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defmethod position-entity ["rectangle-node" :entity-scope] [entity ref-component-name context left top coord-mode]
  (let [effective-offset (calculate-effective-offset entity ref-component-name left top coord-mode)]
    (doseq [component (e/components entity)]
      (let [effective-left  (+ (d/getp (:drawable component) :left) (:left effective-offset))
            effective-top   (+ (d/getp (:drawable component) :top) (:top effective-offset))]
        (when-not (= (:name component) ref-component-name)
          (position-entity-component entity (:name component) context effective-left effective-top :absolute))))
    (position-attributes-components (:attributes entity) (:left effective-offset) (:top effective-offset))))

(defmethod position-entity-component [ "rectangle-node" :main :relation-scope] [entity component-name context left top coord-mode])

(defmethod position-entity-component [ "rectangle-node" :endpoint :relation-scope] [entity component-name context left top coord-mode])

(defmethod position-entity ["rectangle-node" :relation-scope] [entity ref-component-name context left top coord-mode])

;; ==============================================================================================
;; Multimethods for RELATION entity positioning
;; ==============================================================================================

(defmethod get-refered-component-name "relation" [relation]
   (:association-data relation))

(defmethod position-entity-component [ "relation" :endpoint   :relation-scope] [entity component-name context left top coord-mode]
  (position-endpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :startpoint :relation-scope] [entity component-name context left top coord-mode]
  (position-startpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :breakpoint :relation-scope] [entity component-name context left top coord-mode]
  (position-breakpoint entity component-name left top coord-mode))

(defmethod position-entity [ "relation" :relation-scope] [entity ref-component-name context left top coord-mode]
  (position-entity-component entity ref-component-name context left top coord-mode))

(defmethod position-entity-component [ "relation" :endpoint   :entity-scope] [entity component-name context left top coord-mode]
  (position-endpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :startpoint :entity-scope] [entity component-name context left top coord-mode]
  (position-startpoint entity left top coord-mode))

(defmethod position-entity-component [ "relation" :breakpoint :entity-scope] [entity component-name context left top coord-mode]
  (position-breakpoint entity component-name left top coord-mode))

(defmethod position-entity-component [ "relation" :decorator  :entity-scope] [entity component-name context left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defmethod position-entity-component [ "relation" :relation  :entity-scope] [entity component-name context left top coord-mode])

(defmethod position-entity ["relation" :entity-scope] [entity ref-component-name context left top coord-mode]
  (let [effective-offset (calculate-effective-offset entity ref-component-name left top coord-mode)]
    (doseq [component (e/components entity)]
      (when-not (= (:name component) ref-component-name)
        (let [effective-left  (+ (d/get-left (:drawable component)) (:left effective-offset))
              effective-top   (+ (d/get-top (:drawable component)) (:top effective-offset))]
           (position-entity-component entity (:name component) context effective-left effective-top :absolute))))
    (position-attributes-components (:attributes entity) (:left effective-offset) (:top effective-offset))))

(defn moving-entity []
  (fn [e]
    (let [entity (:entity e)
          event (:event e)
          component-name (:component e)
          movementX (:movement-x e)
          movementY (:movement-y e)]
      (position-entity entity
                       component-name
                       :entity-scope
                       movementX
                       movementY
                       :offset)
      (doseq [relation (:relationships entity)]
          (let [related-entity (e/entity-by-id (:entity-id relation))
                ref-component-name (get-refered-component-name relation)]
             (position-entity-component related-entity
                                        ref-component-name
                                        :relation-scope
                                        movementX
                                        movementY
                                        :offset))))))

(defn assert-drawable [event name]
  (= (name (:component event))))

(defn insert-breakpoint []
  (fn [e]
    (when-not (and  (= (:type (p/prev-event e)) "object:moving")
                    (= (:drawable (p/prev-event e)) (:drawable e)))
      (let [entity (:entity e)
            line (e/get-entity-drawable entity (:drawable e))
            line-start-breakpoint (e/get-entity-drawable entity (:start (:props line)))
            line-end-breakpoint   (e/get-entity-drawable entity (:end (:props line)))
            src  (:src line)
            oeX  (.-x2 src)
            oeY  (.-y2 src)
            eX   (.-layerX (.-e (:event e)))
            eY   (.-layerY (.-e (:event e)))]
        (when (= :relation (:type line))
          (.set src (clj->js {:x2 eX :y2 eY}))
          (.setCoords src)
          (let [relation-id   (str (random-uuid))
                breakpoint-id (str (random-uuid))
                is-penultimate (= true (:penultimate (:props line-start-breakpoint)))]
            (e/add-entity-drawable entity
              {:name  relation-id
               :type  :relation
               :src   (relation-line eX eY oeX oeY CONNECTOR_DEFAULT_OPTIONS)
               :props {:start breakpoint-id :end (:name line-end-breakpoint)}}
              {:name  breakpoint-id
               :type  :breakpoint
               :src   (endpoint [eX eY] :moveable true :display "circle" :visible true :opacity 1)
               :props {:end (:name line) :start relation-id :penultimate is-penultimate}})
            (p/sync-entity (e/entity-by-id (:uid entity)))
            (e/update-drawable-prop entity (:name line) :end breakpoint-id)
            (e/update-drawable-prop entity (:name line-end-breakpoint) :end relation-id)
            (when (= true is-penultimate)
              (e/update-drawable-prop entity (:name line-start-breakpoint) :penultimate false))))))))

(defn dissoc-breakpoint []
  (fn [e]
   (let [prev (p/prev-event)]
     (when (and  (not= (:type prev) "object:moving"))
      (let [entity     (:entity e)
            breakpoint (e/get-entity-drawable entity (:drawable e))
            line-end   (e/get-entity-drawable entity (:start  (:props breakpoint)))
            line-endpoint (e/get-entity-drawable entity (:end (:props line-end)))
            line-start (e/get-entity-drawable entity (:end   (:props breakpoint)))
            line-startpoint (e/get-entity-drawable entity (:start (:props line-start)))
            is-penultimate? (:penultimate (:props breakpoint))]
         (e/remove-entity-drawable entity (:name breakpoint))
         (e/remove-entity-drawable entity (:name line-end))
         (e/update-drawable-prop entity (:name line-start) :end (:name line-endpoint))
         (e/update-drawable-prop entity (:name line-endpoint) :end (:name line-start))
         (e/update-drawable-prop entity (:name line-startpoint) :penultimate is-penultimate?)
         (position-entity-drawable entity (:name line-endpoint) :entity-scope (.-left (:src line-endpoint))
                                                                              (.-top  (:src line-endpoint)))
         (p/sync-entity (e/entity-by-id (:uid entity))))))))

; by default validate relations depending on entity bounding box intersection rule.
(defn relations-validate [entity]
  (doseq [relation (:relationships entity)]
    (let [related-entity (e/entity-by-id (:entity-id relation))
          target-bbox (layouts/get-bbox related-entity)
          source-bbox (layouts/get-bbox entity)]
      (when (not (layouts/intersects? source-bbox target-bbox))
        (e/disconnect-entities entity related-entity)))))

(defn- refresh-arrow-angle [relation-drawable arrow-drawable]
  (let [x1 (-> relation-drawable :src (.-x1))
        y1 (-> relation-drawable :src (.-y1))
        x2 (-> relation-drawable :src (.-x2))
        y2 (-> relation-drawable :src (.-y2))]
     (.set (:src arrow-drawable) (clj->js {:angle (calculate-angle x1 y1 x2 y2)}))))

(defn- to-the-center-of [line x y shape]
  (.set line (clj->js {x (+ (.-left shape) (/ (.-width shape) 2))
                       y (+ (.-top shape) (/ (.-height shape) 2))}))
  (.setCoords line))

(defn position-breakpoint
  ([entity name left top coord-mode]
   (let [breakpoint-drawable (e/get-entity-drawable entity name)
         position (effective-position breakpoint-drawable left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-drawable (e/get-entity-drawable entity (:start (:props breakpoint-drawable)))
         ends-relation-drawable (e/get-entity-drawable entity (:end (:props breakpoint-drawable)))
         arrow-drawable (e/get-entity-drawable entity "arrow")]
     (.set (:src breakpoint-drawable) (clj->js {:left effective-left :top  effective-top}))
     (.setCoords (:src breakpoint-drawable))
     (to-the-center-of (:src starts-relation-drawable) :x1 :y1 (:src breakpoint-drawable))
     (to-the-center-of (:src ends-relation-drawable)   :x2 :y2 (:src breakpoint-drawable))
     (when (= true (:penultimate (:props breakpoint-drawable)))
       (refresh-arrow-angle starts-relation-drawable arrow-drawable))))
  ([entity name left top]
   (position-breakpoint entity name left top :absolute)))

(defn position-startpoint
  ([entity left top coord-mode]
   (let [startpoint-drawable (e/get-entity-drawable entity "start")
         position (effective-position startpoint-drawable left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         starts-relation-drawable (e/get-entity-drawable entity (:start (:props startpoint-drawable)))
         arrow-drawable (e/get-entity-drawable entity "arrow")]
     (.set (:src startpoint-drawable) (clj->js {:left effective-left :top  effective-top}))
     (.setCoords (:src startpoint-drawable))
     (to-the-center-of (:src starts-relation-drawable) :x1 :y1 (:src startpoint-drawable))
     (when (= true (:penultimate (:props startpoint-drawable)))
       (refresh-arrow-angle starts-relation-drawable arrow-drawable))))
  ([entity left top]
   (position-startpoint entity left top :absolute)))

(defn position-endpoint
  ([entity left top coord-mode]
   (let [endpoint-drawable   (e/get-entity-drawable entity "end")
         position (effective-position endpoint-drawable left top coord-mode)
         effective-left (:x position)
         effective-top  (:y position)
         ends-relation-drawable  (e/get-entity-drawable entity (:end (:props endpoint-drawable)))

         arrow-drawable      (e/get-entity-drawable entity "arrow")]
    (.set (:src endpoint-drawable) (clj->js {:left effective-left  :top  effective-top}))
    (.setCoords (:src endpoint-drawable))
    (to-the-center-of (:src ends-relation-drawable) :x2 :y2 (:src endpoint-drawable))
    (to-the-center-of (:src arrow-drawable) :left :top (:src endpoint-drawable))
    (refresh-arrow-angle ends-relation-drawable arrow-drawable)))
 ([entity left top]
  (position-endpoint entity left top :absolute)))

(defn toggle-endpoints [entity toggle]
  (doseq [drawable (e/components entity)]
    (when (contains? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (:name drawable))
      (let [src (:src drawable)]
         (.set src (clj->js {:visible toggle :borderColor "#ff0000"}))))))

(defn moving-endpoint []
   (fn [e]
      (let [src      (:src e)
            endpoint-name (:drawable e)
            entity   (:entity e)
            endpoint (e/get-entity-drawable entity endpoint-name)]
         (cond
           (= :breakpoint (:type endpoint)) (position-breakpoint entity endpoint-name (.-left src) (.-top src))
           (= :startpoint (:type endpoint)) (position-startpoint entity (.-left src) (.-top src))
           (= :endpoint   (:type endpoint)) (position-endpoint   entity (.-left src) (.-top src))))))

(defmulti endpoint (fn [point & {:keys [moveable display visible]}] display))

(defmethod endpoint "circle" [point & {:keys [moveable display visible opacity]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))
                        :visible visible
                        :opacity opacity}
                       HANDLER_SMALL
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Circle. (clj->js options))))

(defmethod endpoint "rect" [point & {:keys [moveable display visible]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))
                        :width (* 2 (:radius HANDLER_SMALL))
                        :height (* 2 (:radius HANDLER_SMALL))
                        :visible visible}
                       (dissoc HANDLER_SMALL :radius)
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Rect. (clj->js options))))

(defn arrow [data options]
  (let [x1 (+ (:left options))
        y1 (+ (:top options))
        x2 (+ (:left options) (first (last (partition 2 data))))
        y2 (+ (:top options)  (last  (last (partition 2 data))))
        cX (/ (+ x1 x2) 2)
        cY (/ (+ y1 y2) 2)
        deltaX (- x1 cX)
        deltaY (- y1 cY)]
      (js/fabric.Triangle. (clj->js (merge {:left x2
                                            :top (+ y1 deltaY)
                                            :originX "center"
                                            :originY "center"
                                            :angle 90
                                            :width 20
                                            :height 20}
                                           LOCKED_MOVEMENT
                                           RESTRICTED_BEHAVIOUR
                                           NO_DEFAULT_CONTROLS
                                           DEFAULT_STROKE
                                           DEFAULT_FILL)))))

(defn relation-line [x1 y1 x2 y2 options]
  (js/fabric.Line. (clj->js [ x1 y1 x2 y2 ])  (clj->js options)))

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
