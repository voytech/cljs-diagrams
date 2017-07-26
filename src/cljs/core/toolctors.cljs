(ns core.toolctors
 (:require [core.entities :as e])
 (:require-macros [core.macros :refer [defentity]]))

(def DEFAULT_SIZE_OPTS {:width 180 :height 150})
(def TRANSPARENT_FILL {:fill "rgb(255,255,255)"})
(def DEFAULT_STROKE {:stroke "#666" :strokeWidth 1.5})
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false :lockRotation true})
(def NO_DEFAULT_CONTROLS {:hasControls false :hasBorders false})
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "#666" :strokeWidth 1.5})
(def DEFAULT_OPTIONS {:highlight-color "red"
                      :normal-color "#666"
                      :highlight-width 3
                      :normal-width 1.5})
;; Below is an interface to js Fabric.js library.

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

(defn overlaying? [src trg]
    (or (.intersectsWithObject src trg)
        (.isContainedWithinObject src trg)
        (.isContainedWithinObject trg src)))

(defn intersects-any? [parts yes]
  (fn [e]
    (let [src    (:src e)
          entity (:entity e)
          part   (:part e)
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
                                 (yes {:src src :part src-part :entity src-ent} {:src trg :part trg-part :entity trg-ent})))))))))

(defn intersects? [part_ yes no]
  (fn [e]
    (let [src    (:src e)
          entity (:entity e)
          part   (:part e)
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
                                (yes {:src src :part src-part :entity src-ent} {:src trg :part trg-part :entity trg-ent})
                                (no  {:src src :part src-part :entity src-ent} {:src trg :part trg-part :entity trg-ent})))))))))

(defn moving-entity [part-name]
  (fn [e]
    (when (= (:part e) part-name)
      (let [entity (:entity e)
            event (:event e)
            movementX (.-movementX (.-e event))
            movementY (.-movementY (.-e event))]
        (doseq [part (:parts entity)]
          (when (not (= (:name part) part-name))
            (.set (:src part) (clj->js {:left (+ (.-left (:src part)) movementX)
                                        :top  (+ (.-top (:src part)) movementY)}))
            (.setCoords (:src part))))
        (doseq [relation (:relationships entity)]
            (let [end (:end relation)
                  related-entity (e/entity-by-id (:entity-id relation))
                  related-part   (e/entity-part related-entity (name end))
                  related-cl     (e/entity-part related-entity "connector")]
              (.set (:src related-part) (clj->js {:left (+ (.-left (:src related-part)) movementX)
                                                  :top  (+ (.-top (:src related-part)) movementY)}))
              (.setCoords (:src related-part))
              (.set (:src related-cl)
                (if (= "start" end)
                  (clj->js {:x1 (+ (.-x1 (:src related-cl)) movementX)
                            :y1  (+ (.-y1 (:src related-cl)) movementY)})
                  (clj->js {:x2 (+ (.-x2 (:src related-cl)) movementX)
                            :y2  (+ (.-y2 (:src related-cl)) movementY)})))
              (.setCoords (:src related-cl))))))))


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
          end-part   (e/entity-part entity end)
          end-src    (:src end-part)
          related-entity (e/entity-by-id (:entity-id relation))
          cnt (count (:parts related-entity))
          i (atom 0)]
        (doseq [part (:parts related-entity)]
          (let [related-part-src (:src part)]
            (if (not (overlaying? end-src related-part-src))
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

(defn position-endpoint [entity terminator-end left top]
  (let [terminator-part   (e/entity-part entity terminator-end)
        relation-part     (e/entity-part entity "connector")]
    (.set (:src terminator-part) (clj->js {:left left
                                           :top  top}))
    (.setCoords (:src terminator-part))
    (.set (:src relation-part)
      (if (= "start" terminator-end)
        (clj->js {:x1 (+ (.-left (:src terminator-part)) (/ (.-width  (:src terminator-part)) 2))
                  :y1 (+ (.-top (:src terminator-part)) (/ (.-height (:src terminator-part)) 2))})
        (clj->js {:x2 (+ (.-left (:src terminator-part)) (/ (.-width  (:src terminator-part)) 2))
                  :y2 (+ (.-top (:src terminator-part)) (/ (.-height (:src terminator-part)) 2))})))
    (.setCoords (:src relation-part))))

(defn toggle-connectors [entity toggle]
  (doseq [part (:parts entity)]
    (when (contains? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (:name part))
      (let [src (:src part)]
         (.set src (clj->js {:visible toggle :borderColor "#ff0000"}))))))

(defn moving-connector [coordX coordY]
   (fn [e]
      (let [src (:src e)
            entity (:entity e)
            connector (e/entity-part entity "connector")]
         (.set (:src connector) (clj->js {(keyword coordX) (+ (.-left src) 8)
                                          (keyword coordY) (+ (.-top src) 8)}))
         (.setCoords (:src connector)))))


(defmulti connector (fn [point & {:keys [moveable display visible]}] display))

(defmethod connector "circle" [point & {:keys [moveable display visible]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))
                        :visible visible}
                       HANDLER_SMALL
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Circle. (clj->js options))))

(defmethod connector "rect" [point & {:keys [moveable display visible]}]
  (let [options (merge {:left (- (first point) (:radius HANDLER_SMALL))
                        :top (- (last point)   (:radius HANDLER_SMALL))
                        :width (* 2 (:radius HANDLER_SMALL))
                        :height (* 2 (:radius HANDLER_SMALL))
                        :visible visible}
                       (dissoc HANDLER_SMALL :radius)
                       NO_DEFAULT_CONTROLS)]
      (js/fabric.Rect. (clj->js options))))


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
      ["connector-left"   (connector conL :moveable false :display "rect" :visibile false)
       "connector-right"  (connector conR :moveable false :display "rect" :visibile false)
       "connector-top"    (connector conT :moveable false :display "rect" :visibile false)
       "connector-bottom" (connector conB :moveable false :display "rect" :visibile false)
       "body"             (js/fabric.Rect. (clj->js enriched-opts))]))
  (with-behaviours
    ["body" "object:moving" (moving-entity "body")
     "body" "mouse:over" (highlight true DEFAULT_OPTIONS)
     "body" "mouse:out" (highlight false DEFAULT_OPTIONS)]))


(defentity relation data options
  (with-drawables
    (let [enriched-opts (merge options DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS)
          offset-x (:left options)
          offset-y (:top options)
          points-pairs (partition 2 data)
          points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
          conS (first points-pairs-offset)
          conE (last points-pairs-offset)]
        ["connector" (js/fabric.Line. (clj->js (flatten points-pairs-offset)) (clj->js enriched-opts))
         "start"     (connector conS :moveable true :display "circle" :visible true)
         "end"       (connector conE :moveable true :display "circle" :visible true)]))
  (with-behaviours
    ["start" "object:moving"  (all (moving-connector "x1" "y1")
                                   (intersects? "body" (fn [src trg] (toggle-connectors (:entity trg) true))
                                                       (fn [src trg] (toggle-connectors (:entity trg) false))))
     "start" "mouse:up"       (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) (:part src))
                                                                                                                                           (toggle-connectors (:entity trg) false)
                                                                                                                                           (position-endpoint (:entity src) "start" (.-left (:src trg)) (.-top (:src trg)))))
                                   (for-entity relations-validate))
     "start" "mouse:over"     (highlight true DEFAULT_OPTIONS)
     "start" "mouse:out"      (highlight false DEFAULT_OPTIONS)

     "end"   "object:moving"  (all (moving-connector "x2" "y2")
                                   (intersects? "body" (fn [src trg] (toggle-connectors (:entity trg) true))
                                                       (fn [src trg] (toggle-connectors (:entity trg) false))))
     "end"   "mouse:up"       (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) (:part src))
                                                                                                                                           (toggle-connectors (:entity trg) false)
                                                                                                                                           (position-endpoint (:entity src) "end" (.-left (:src trg)) (.-top (:src trg)))))
                                   (for-entity relations-validate))
     "end" "mouse:over"       (highlight true DEFAULT_OPTIONS)
     "end" "mouse:out"        (highlight false DEFAULT_OPTIONS)]))

(defn create
  ([entity data]
   (fn [context]
     (entity data context)))
  ([entity]
   (fn [context]
     (entity nil context))))
