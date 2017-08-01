(ns core.toolctors
 (:require [core.entities :as e]
           [core.project :as p]
           [clojure.string :as str])
 (:require-macros [core.macros :refer [defentity]]))

(declare position-endpoint)
(declare moving-endpoint)
(declare relation-line)
(declare endpoint)
(declare calculate-angle)

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

(defn image [data options]
  (if (not (nil? options))
    (js/fabric.Image. data (clj->js options))
    (js/fabric.Image. data)))

(defn highlight [bln options]
  (fn [e]
    (.set (:src e) (clj->js {:stroke (if bln (:highlight-color options)
                                             (:normal-color options))
                             :strokeWidth (if bln (:highlight-width options)
                                                  (:normal-width options))}))))

(defn is-break-point [x1 y1 x2 y2 bx by]
  (let [dx (- x2 x1)
        dy (- y2 y2)
        l (- (* by dx) (* y1 dx))
        r (- (* bx dy) (* x1 dy))]
       (= l r)))

(defn test-break-point []
  (fn [e]
    (let [entity (:entity e)
          start-connector (e/get-entity-drawable entity "start")
          end-connector (e/get-entity-drawable entity "end")
          sc-src (:src start-connector)
          ec-src (:src end-connector)]
        (js/console.log  (.-e (:event e))))))


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


(defmulti get-related-entitity-drawable (fn [relation] (:type (e/entity-by-id (:entity-id relation)))))

(defmulti position-entity-drawable (fn [entity drawable] [(:type entity) (:type drawable)]))


(defmethod get-related-entitity-drawable "rectangle-node" [relation]
  (let [related-entity (e/entity-by-id (:entity-id relation))]
    (e/get-entity-drawable related-entity "body")))

(defmethod position-entity-drawable [ "rectangle-node" :main ] [entity drawable left top])

(defmethod get-related-entitity-drawable "relation" [relation]
  (let [related-entity (e/entity-by-id (:entity-id relation))]
    (e/get-entity-drawable related-entity (:end relation))))

(defmethod position-entity-drawable [ "relation" :endpoint ] [entity drawable left top]
  (position-endpoint entity (:name drawable) left top))

(defmethod position-entity-drawable [ "relation" :startpoint ] [entity drawable left top]
  (position-endpoint entity (:name drawable) left top))

(defmethod position-entity-drawable [ "relation" :breakpoint ] [entity drawable left top]
  (position-endpoint entity (:name drawable) left top))

(defn moving-entity [drawable-name]
  (fn [e]
    (when (= (:drawable e) drawable-name)
      (let [entity (:entity e)
            event (:event e)
            movementX (.-movementX (.-e event))
            movementY (.-movementY (.-e event))]
        (doseq [drawable (:drawables entity)]
          (when (not (= (:name drawable) drawable-name))
            (.set (:src drawable) (clj->js {:left (+ (.-left (:src drawable)) movementX)
                                            :top  (+ (.-top (:src drawable)) movementY)}))
            (.setCoords (:src drawable))))
        (doseq [relation (:relationships entity)]
            (let [related-entity (e/entity-by-id (:entity-id relation))
                  drawable (get-related-entitity-drawable relation)]
               (position-entity-drawable related-entity drawable (-> drawable :src (.-left) (+ movementX))
                                                                 (-> drawable :src (.-top)  (+ movementY)))))))))

(defn assert-drawable [event name]
  (= (name (:drawable event))))

(defn break-line []
  (fn [e]
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
             :behaviours {"mouse:up" (break-line)}}
            {:name  breakpoint-id
             :type  :breakpoint
             :src   (endpoint [eX eY] :moveable true :display "circle" :visible true :opacity 1)
             :rels {:end (:name line) :start relation-id :penultimate is-penultimate}
             :behaviours {"mouse:over"    (highlight true DEFAULT_OPTIONS)
                          "mouse:out"     (highlight false DEFAULT_OPTIONS)
                          "object:moving" (moving-endpoint)}})
          (p/sync-entity (e/entity-by-id (:uid entity)))
          (e/update-drawable-rel entity (:name line) :end breakpoint-id)
          (e/update-drawable-rel entity (:name line-end-breakpoint) :end relation-id)
          (when (= true is-penultimate)
            (e/update-drawable-rel entity (:name line-start-breakpoint) :penultimate false)))))))


(defn all [ & handlers]
  (fn [e]
    (doseq [handler handlers]
      (handler e))))

(defn for-entity [f]
  (fn [e]
    (let [entity (:entity e)]
      (f entity))))

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
  ([entity endpoint-name left top]
   (let [endpoint-drawable   (e/get-entity-drawable entity endpoint-name)
         starts-relation-drawable  (if-let [name (:start (:rels endpoint-drawable))]
                                     (e/get-entity-drawable entity name)
                                     nil)
         ends-relation-drawable  (if-let [name (:end (:rels endpoint-drawable))]
                                     (e/get-entity-drawable entity name)
                                     nil)
         arrow-drawable      (e/get-entity-drawable entity "arrow")]
    (.set (:src endpoint-drawable) (clj->js {:left left
                                             :top  top}))
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
         (.set (:src arrow-drawable) (clj->js {:angle (calculate-angle x1 y1 x2 y2)})))))))


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

(defentity rectangle-node data options
  (with-drawables
    (let [enriched-opts (merge options
                               DEFAULT_SIZE_OPTS
                               TRANSPARENT_FILL
                               DEFAULT_STROKE
                               RESTRICTED_BEHAVIOUR
                               NO_DEFAULT_CONTROLS)
          conL    (vector (:left options) (+ (/ (:height DEFAULT_SIZE_OPTS) 2) (:top options)))
          conR    (vector (+ (:left options) (:width DEFAULT_SIZE_OPTS)) (+ (/ (:height DEFAULT_SIZE_OPTS) 2) (:top options)))
          conT    (vector (+ (/ (:width DEFAULT_SIZE_OPTS) 2) (:left options)) (:top options))
          conB    (vector (+ (/ (:width DEFAULT_SIZE_OPTS) 2) (:left options)) (+ (:top options) (:height DEFAULT_SIZE_OPTS)))]
      [{:name "connector-left"
        :type :endpoint
        :src (endpoint conL :moveable false :display "rect" :visibile false)}
       {:name "connector-right"
        :type :endpoint
        :src (endpoint conR :moveable false :display "rect" :visibile false)}
       {:name "connector-top"
        :type :endpoint
        :src (endpoint conT :moveable false :display "rect" :visibile false)}
       {:name "connector-bottom"
        :type :endpoint
        :src (endpoint conB :moveable false :display "rect" :visibile false)}
       {:name "body"
        :type :main
        :src (js/fabric.Rect. (clj->js enriched-opts))
        :behaviours {"object:moving" (moving-entity "body")
                     "mouse:over"    (highlight true DEFAULT_OPTIONS)
                     "mouse:out"     (highlight false DEFAULT_OPTIONS)}}])))

(defentity relation data options
  (with-drawables
    (let [enriched-opts (merge options DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS)
          offset-x (:left options)
          offset-y (:top options)
          points-pairs (partition 2 data)
          points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
          conS (first points-pairs-offset)
          conE (last points-pairs-offset)]
        [{:name "connector"
          :type :relation
          :src  (relation-line (first conS) (last conS) (first conE) (last conE) enriched-opts)
          :behaviours {"mouse:up" (break-line)}
          :rels {:start "start" :end "end"}}

         {:name "start"
          :type :startpoint
          :src  (endpoint conS :moveable true :display "circle" :visible true :opacity 1)
          :behaviours {"object:moving" (all (moving-endpoint)
                                            (intersects? "body" (fn [src trg] (toggle-endpoints (:entity trg) true))
                                                                (fn [src trg] (toggle-endpoints (:entity trg) false))))
                       "mouse:up"      (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) (:drawable src))
                                                                                                                                                    (toggle-endpoints (:entity trg) false)
                                                                                                                                                    (position-endpoint (:entity src) "start" (.-left (:src trg)) (.-top (:src trg)))))
                                            (for-entity relations-validate))
                       "mouse:over"    (highlight true DEFAULT_OPTIONS)
                       "mouse:out"     (highlight false DEFAULT_OPTIONS)}
          :rels {:start "connector" :penultimate true}}

         {:name "arrow"
          :type :decorator
          :src  (arrow data options)}

         {:name "end"
          :type :endpoint
          :src  (endpoint conE :moveable true :display "circle" :visible true :opacity 0)
          :behaviours {"object:moving" (all (moving-endpoint)
                                            (intersects? "body" (fn [src trg] (toggle-endpoints (:entity trg) true))
                                                                (fn [src trg] (toggle-endpoints (:entity trg) false))))
                       "mouse:up"      (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) (:drawable src))
                                                                                                                                                    (toggle-endpoints (:entity trg) false)
                                                                                                                                                    (position-endpoint (:entity src) "end" (.-left (:src trg)) (.-top (:src trg)))))
                                            (for-entity relations-validate))
                       "mouse:over"    (highlight true DEFAULT_OPTIONS)
                       "mouse:out"     (highlight false DEFAULT_OPTIONS)}
          :rels {:end "connector"}}])))

(defn create
  ([entity data]
   (fn [context]
     (entity data context)))
  ([entity]
   (fn [context]
     (entity nil context))))
