(ns core.entities-behaviours
 (:require [core.entities :as e]
           [core.project :as p]
           [clojure.string :as str]))


(declare position-endpoint)
(declare moving-endpoint)
(declare relation-line)
(declare endpoint)
(declare calculate-angle)
(declare dissoc-breakpoint)

(def DEFAULT_SIZE_OPTS {:width 180 :height 150})
(def TRANSPARENT_FILL {:fill "rgb(255,255,255)"})
(def DEFAULT_FILL {:fill "#666"})
(def DEFAULT_STROKE {:stroke "#666" :strokeWidth 1.5})
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false
                           :lockRotation true
                           :lockScalingX true
                           :lockScalingY true})
(def LOCKED_MOVEMENT      {:lockMovementX true
                           :lockMovementY true})
(def NO_DEFAULT_CONTROLS {:hasControls false :hasBorders false})
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "#666" :strokeWidth 1.5})
(def HANDLER_SMALLEST {:radius 8 :fill "#fff" :stroke "#666" :strokeWidth 1.5})
(def DEFAULT_OPTIONS {:highlight-color "red"
                      :normal-color "#666"
                      :highlight-width 3
                      :normal-width 1.5})
(def CONNECTOR_DEFAULT_OPTIONS (merge DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS))

(defn highlight [bln options]
  (fn [e]
    (.set (:src e) (clj->js {:stroke (if bln (:highlight-color options)
                                             (:normal-color options))
                             :strokeWidth (if bln (:highlight-width options)
                                                  (:normal-width options))}))))

(defn overlaying? [src trg]
    (or (.intersectsWithObject src trg)
        (.isContainedWithinObject src trg)
        (.isContainedWithinObject trg src)))

(defn intersects-any? [parts yes]
  (fn [e]
    (let [src    (:src e)
          entity (:entity e)
          part   (:drawable e)
          canvas (:canvas e)]
      (when (contains? #{"end" "start"} part)
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
     (Error. "context must be entity-relation or drawable-relation")))

(defn- calculate-offset [drawable left top]
  {:left (- left (.-left  (:src drawable)))
   :top  (- top  (.-top   (:src drawable)))})

(defn- calculate-effective-offset [entity drawable-name left top coord-mode]
  (if (= :offset coord-mode)
    {:left left :top top}
    (let [drawable  (e/get-entity-drawable entity drawable-name)]
      (calculate-offset drawable left top))))

(defn- effective-position [drawable left top coord-mode]
  (let [effective-left (if (= :offset coord-mode) (+ (.-left (:src drawable)) left) left)
        effective-top  (if (= :offset coord-mode) (+ (.-top (:src drawable)) top) top)]
    {:left effective-left :top effective-top}))

(defn- apply-effective-position [drawable left top coord-mode]
  (let [epos (effective-position drawable left top coord-mode)]
    (.set (:src drawable) (clj->js {:left (:left epos)
                                    :top  (:top epos)}))
    (.setCoords (:src drawable))))


(defmulti get-refered-drawable-name (fn [relation] (:type (e/entity-by-id (:entity-id relation)))))

(defmulti position-entity-drawable (fn [entity drawable-name context left top coord-mode]
                                     (assert-position-context context)
                                     [(:type entity) (:type (e/get-entity-drawable entity drawable-name)) context]))

(defmulti position-entity (fn [entity ref-drawable-name context left top coord-mode]
                            (assert-position-context context)
                            [(:type entity) context]))

;; ==============================================================================================
;; Multimethods for RECTANGLE-NODE entity positioning
;; ==============================================================================================

(defmethod get-refered-drawable-name "rectangle-node" [relation]
   "body")

(defmethod position-entity-drawable [ "rectangle-node" :main :entity-scope] [entity drawable-name context left top coord-mode]
  (let [drawable (e/get-entity-drawable entity drawable-name)]
    (apply-effective-position drawable left top coord-mode)))

(defmethod position-entity-drawable [ "rectangle-node" :endpoint :entity-scope] [entity drawable-name context left top coord-mode]
  (let [drawable (e/get-entity-drawable entity drawable-name)]
    (apply-effective-position drawable left top coord-mode)))

(defmethod position-entity ["rectangle-node" :entity-scope] [entity ref-drawable-name context left top coord-mode]
  (let [effective-offset (calculate-effective-offset entity ref-drawable-name left top coord-mode)]
    (doseq [drawable (:drawables entity)]
      (let [effective-left  (+ (.-left (:src drawable)) (:left effective-offset))
            effective-top   (+ (.-top (:src drawable)) (:top effective-offset))]
        (when-not (= (:name drawable) ref-drawable-name)
          (position-entity-drawable entity (:name drawable) context effective-left effective-top :absolute))))))

(defmethod position-entity-drawable [ "rectangle-node" :main :relation-scope] [entity drawable-name context left top coord-mode])

(defmethod position-entity-drawable [ "rectangle-node" :endpoint :relation-scope] [entity drawable-name context left top coord-mode])

(defmethod position-entity ["rectangle-node" :relation-scope] [entity ref-drawable-name context left top coord-mode])

;; ==============================================================================================
;; Multimethods for RELATION entity positioning
;; ==============================================================================================

(defmethod get-refered-drawable-name "relation" [relation]
   (:end relation))

(defmethod position-entity-drawable [ "relation" :endpoint   :relation-scope] [entity drawable-name context left top coord-mode]
  (position-endpoint entity drawable-name left top coord-mode))

(defmethod position-entity-drawable [ "relation" :startpoint :relation-scope] [entity drawable-name context left top coord-mode]
  (position-endpoint entity drawable-name left top coord-mode))

(defmethod position-entity-drawable [ "relation" :breakpoint :relation-scope] [entity drawable-name context left top coord-mode]
  (position-endpoint entity drawable-name left top coord-mode))

(defmethod position-entity [ "relation" :relation-scope] [entity ref-drawable-name context left top coord-mode]
  (position-entity-drawable entity ref-drawable-name context left top coord-mode))

(defmethod position-entity-drawable [ "relation" :endpoint   :entity-scope] [entity drawable-name context left top coord-mode]
  (position-endpoint entity drawable-name left top coord-mode))

(defmethod position-entity-drawable [ "relation" :startpoint :entity-scope] [entity drawable-name context left top coord-mode]
  (position-endpoint entity drawable-name left top coord-mode))

(defmethod position-entity-drawable [ "relation" :breakpoint :entity-scope] [entity drawable-name context left top coord-mode]
  (position-endpoint entity drawable-name left top coord-mode))

(defmethod position-entity-drawable [ "relation" :decorator  :entity-scope] [entity drawable-name context left top coord-mode]
  (let [drawable (e/get-entity-drawable entity drawable-name)]
    (apply-effective-position drawable left top coord-mode)))

(defmethod position-entity-drawable [ "relation" :relation  :entity-scope] [entity drawable-name context left top coord-mode])

(defmethod position-entity ["relation" :entity-scope] [entity ref-drawable-name context left top coord-mode]
  (let [effective-offset (calculate-effective-offset entity ref-drawable-name left top coord-mode)]
    (doseq [drawable (:drawables entity)]
      (when-not (= (:name drawable) ref-drawable-name)
        (let [effective-left  (+ (.-left (:src drawable)) (:left effective-offset))
              effective-top   (+ (.-top (:src drawable)) (:top effective-offset))]
           (position-entity-drawable entity (:name drawable) context effective-left effective-top :absolute))))))

(defn moving-entity [drawable-name]
  (fn [e]
    (when (= (:drawable e) drawable-name)
      (let [entity (:entity e)
            event (:event e)
            movementX (.-movementX (.-e event))
            movementY (.-movementY (.-e event))]
        (position-entity entity
                         drawable-name
                         :entity-scope
                         movementX
                         movementY
                         :offset)
        (doseq [relation (:relationships entity)]
            (let [related-entity (e/entity-by-id (:entity-id relation))
                  ref-drawable-name (get-refered-drawable-name relation)]
               (position-entity-drawable related-entity
                                         ref-drawable-name
                                         :relation-scope
                                         movementX
                                         movementY
                                         :offset)))))))

(defn assert-drawable [event name]
  (= (name (:drawable event))))

(defn insert-breakpoint []
  (fn [e]
    (when-not (and  (= (:type (p/prev-event e)) "object:moving")
                    (= (:drawable (p/prev-event e)) (:drawable e)))
      (let [entity (:entity e)
            line (e/get-entity-drawable entity (:drawable e))
            line-start-breakpoint (e/get-entity-drawable entity (:start (:rels line)))
            line-end-breakpoint   (e/get-entity-drawable entity (:end (:rels line)))
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
                is-penultimate (= true (:penultimate (:rels line-start-breakpoint)))]
            (e/add-entity-drawable entity
              {:name  relation-id
               :type  :relation
               :src   (relation-line eX eY oeX oeY CONNECTOR_DEFAULT_OPTIONS)
               :rels {:start breakpoint-id :end (:name line-end-breakpoint)}
               :behaviours {"mouse:up" (insert-breakpoint)
                            "object:moving" (all (moving-entity relation-id)
                                                 (event-wrap relations-validate))}}
              {:name  breakpoint-id
               :type  :breakpoint
               :src   (endpoint [eX eY] :moveable true :display "circle" :visible true :opacity 1)
               :rels {:end (:name line) :start relation-id :penultimate is-penultimate}
               :behaviours {"mouse:over"    (highlight true DEFAULT_OPTIONS)
                            "mouse:out"     (highlight false DEFAULT_OPTIONS)
                            "mouse:up"      (dissoc-breakpoint)
                            "object:moving" (moving-endpoint)}})
            (p/sync-entity (e/entity-by-id (:uid entity)))
            (e/update-drawable-rel entity (:name line) :end breakpoint-id)
            (e/update-drawable-rel entity (:name line-end-breakpoint) :end relation-id)
            (when (= true is-penultimate)
              (e/update-drawable-rel entity (:name line-start-breakpoint) :penultimate false))))))))

(defn dissoc-breakpoint []
    (fn [e]
     (let [prev (p/prev-event)]
       (when (and  (not= (:type prev) "object:moving"))
        (let [entity     (:entity e)
              breakpoint (e/get-entity-drawable entity (:drawable e))
              line-end   (e/get-entity-drawable entity (:start     (:rels breakpoint)))
              line-endpoint (e/get-entity-drawable entity (:end (:rels line-end)))
              line-start (e/get-entity-drawable entity (:end   (:rels breakpoint)))
              line-startpoint (e/get-entity-drawable entity (:start (:rels line-start)))
              is-penultimate? (:penultimate (:rels breakpoint))]
           (e/remove-entity-drawable entity (:name breakpoint))
           (e/remove-entity-drawable entity (:name line-end))
           (e/update-drawable-rel entity (:name line-start) :end (:name line-endpoint))
           (e/update-drawable-rel entity (:name line-endpoint) :end (:name line-start))
           (e/update-drawable-rel entity (:name line-startpoint) :penultimate is-penultimate?)
           (position-entity-drawable entity (:name line-endpoint) :entity-scope (.-left (:src line-endpoint))
                                                                                (.-top  (:src line-endpoint)))
           (p/sync-entity (e/entity-by-id (:uid entity))))))))

(defn all [ & handlers]
  (fn [e]
    (doseq [handler handlers]
      (handler e))))

(defn event-wrap
  ([f]
   (fn [e]
     (let [entity (:entity e)]
       (f entity))))
  ([f & args]
   (fn [e]
     (let [entity (:entity e)
           drawable-name (:drawabe e)]
       (apply f (cons entity (cons drawable-name (vec args))))))))


(defn relations-validate [entity]
  (doseq [relation (:relationships entity)]
    (let [end (:end relation)
          end-part   (e/get-entity-drawable entity end)
          end-src    (:src end-part)
          related-entity (e/entity-by-id (:entity-id relation))
          cnt (count (:drawables related-entity))
          i (atom 0)]
        (doseq [drawable (:drawables related-entity)]
          (let [related-d-src (:src drawable)]
            (if (not (overlaying? end-src related-d-src))  ; use filter instead of doseq here to make it more declaratice
              (swap! i inc))))
        (when (= @i cnt)
          (e/disconnect-entities entity related-entity)))))


(defn align-center [src trg]
  (let [srcCx   (+ (.-left src) (/ (.-width src) 2))
        srcCy   (+ (.-top src) (/ (.-width src) 2))
        trgLeft (- srcCx (/ (.-width trg) 2))
        trgTop  (- srcCy (/ (.-height trg) 2))]
      (.set trg (clj->js {:left trgLeft :top trgTop}))
      (.setCoords trg)))

(defn position-endpoint
  ([entity endpoint-name left top coord-mode]
   (let [endpoint-drawable   (e/get-entity-drawable entity endpoint-name)
         effective-left (if (= coord-mode :offset) (+ (.-left (:src endpoint-drawable)) left)  left)
         effective-top  (if (= coord-mode :offset) (+ (.-top (:src endpoint-drawable)) top)  top)
         starts-relation-drawable  (if-let [name (:start (:rels endpoint-drawable))]
                                     (e/get-entity-drawable entity name)
                                     nil)
         ends-relation-drawable  (if-let [name (:end (:rels endpoint-drawable))]
                                     (e/get-entity-drawable entity name)
                                     nil)
         arrow-drawable      (e/get-entity-drawable entity "arrow")]
    (.set (:src endpoint-drawable) (clj->js {:left effective-left
                                             :top  effective-top}))
    (.setCoords (:src endpoint-drawable))
    (when-not (nil? starts-relation-drawable)
      (.set (:src starts-relation-drawable) (clj->js {:x1 (+ (.-left (:src endpoint-drawable)) (/ (.-width  (:src endpoint-drawable)) 2))
                                                      :y1 (+ (.-top (:src endpoint-drawable)) (/ (.-height (:src endpoint-drawable)) 2))}))
      (.setCoords (:src starts-relation-drawable)))
    (when-not (nil? ends-relation-drawable)
      (.set (:src ends-relation-drawable) (clj->js {:x2 (+ (.-left (:src endpoint-drawable)) (/ (.-width  (:src endpoint-drawable)) 2))
                                                    :y2 (+ (.-top (:src endpoint-drawable)) (/ (.-height (:src endpoint-drawable)) 2))}))
      (.setCoords (:src ends-relation-drawable)))

    (if (or  (= "end" endpoint-name) (= :endpoint (:type endpoint-drawable)))
      (.set (:src arrow-drawable) (clj->js {:left (.-x2 (:src ends-relation-drawable))
                                            :top  (.-y2 (:src ends-relation-drawable))}))
      (.setCoords (:src arrow-drawable)))
    (if (or  (= "end" endpoint-name)
             (= :endpoint (:type endpoint-drawable))
             (= true (:penultimate (:rels endpoint-drawable))))

      (let [relation (if (= true (:penultimate (:rels endpoint-drawable)))
                        starts-relation-drawable
                        (if (= :endpoint (:type endpoint-drawable))
                          ends-relation-drawable))
            x1 (-> relation :src (.-x1))
            y1 (-> relation :src (.-y1))
            x2 (-> relation :src (.-x2))
            y2 (-> relation :src (.-y2))]
         (.set (:src arrow-drawable) (clj->js {:angle (calculate-angle x1 y1 x2 y2)}))))))
 ([entity endpoint-name left top]
  (position-endpoint entity endpoint-name left top :absolute)))

(defn show [entity drawable-name show]
  (let [drawable (e/get-entity-drawable entity drawable-name)]
    (.set (:src drawable) (clj->js {:visible show}))))

(defn toggle-endpoints [entity toggle]
  (doseq [drawable (:drawables entity)]
    (when (contains? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (:name drawable))
      (let [src (:src drawable)]
         (.set src (clj->js {:visible toggle :borderColor "#ff0000"}))))))

(defn moving-endpoint []
   (fn [e]
      (let [src      (:src e)
            endpoint (:drawable e)
            entity   (:entity e)]
         (position-endpoint entity endpoint (.-left src) (.-top  src)))))

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
