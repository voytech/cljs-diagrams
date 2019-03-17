(ns cljs-diagrams.core.behaviour-api
  (:require [cljs-diagrams.core.nodes :as e]
            [cljs-diagrams.core.layouts :as layouts]
            [cljs-diagrams.core.shapes :as d]
            [cljs-diagrams.core.eventbus :as b]
            [cljs-diagrams.core.events :as ev]
            [cljs-diagrams.core.state :as state]
            [cljs-diagrams.core.behaviours :as bhv]
            [cljs-diagrams.impl.std.shapes :as c]))

(defn- center-point [cmp]
  (let [mx (+ (d/get-left cmp) (/ (d/get-width cmp) 2))
        my (+ (d/get-top cmp) (/ (d/get-height cmp) 2))]
    {:x mx :y my}))

(defn- distance [p1 p2]
  (js/Math.sqrt (+ (js/Math.pow (- (:x p2) (:x p1)) 2) (js/Math.pow (- (:y p2) (:y p1)) 2))))

(defn to-the-center-of [shape x y target]
  (when (not (nil? shape))
    (d/set-data shape {x (+ (d/get-left target) (/ (d/get-width target) 2))
                       y (+ (d/get-top target) (/ (d/get-height target) 2))})))

(defn shape-hover [node shape bln options]
  (let [original-model (e/preset-shapes-properties node (:name shape))
        normal-color (or (:border-color original-model) (:normal-color options))
        normal-width (or (:border-width original-model) (:normal-width options))]
    (d/set-data shape {:border-color (if bln (:hover-color options)
                                             normal-color)
                       :border-width (if bln (:hover-width options)
                                             normal-width)})))

(defn show [app-state node shape-name show]
 (let [shape (e/get-node-shape node shape-name)]
   (d/setp shape :visible show)))

(defn calculate-offset [shape left top]
 {:left (- left (d/getp shape :left))
  :top  (- top  (d/getp shape :top))})

(defn calculate-effective-offset [app-state node shape-name left top coord-mode]
 (if (= :offset coord-mode)
   {:left left :top top}
   (let [shape (e/get-node-shape node shape-name)]
     (calculate-offset shape left top))))

(defn effective-position
 ([shape get-x get-y x y coord-mode]
  (let [effective-x (if (= :offset coord-mode) (+ (get-x shape) x) x)
        effective-y  (if (= :offset coord-mode) (+ (get-y shape) y) y)]
    {:x effective-x :y effective-y}))
 ([shape x y coord-mode]
  (effective-position shape #(d/getp % :left) #(d/getp % :top) x y coord-mode)))

(defn apply-effective-position
 ([shape set-x get-x set-y get-y x y coord-mode]
  (let [epos (effective-position shape get-x get-y x y coord-mode)]
    (set-x shape (:x epos))
    (set-y shape (:y epos))))
 ([shape x y coord-mode]
  (apply-effective-position shape
                            #(d/setp %1 :left %2)
                            #(d/getp % :left)
                            #(d/setp %1 :top %2)
                            #(d/getp % :top)
                            x
                            y
                            coord-mode)))

(defn default-position-node-shape [app-state node shape-name left top coord-mode]
  (let [shape (e/get-node-shape node shape-name)]
    (apply-effective-position shape left top coord-mode)))

(defn default-position-node [app-state node ref-shape-name mx my coord-mode]
  (let [bbox (:bbox node)
        {:keys [left top]} bbox]
    (doseq [shape (filterv #(not (some? (-> % :layout-attributes :layout-ref))) (e/shapes-of node))]
      (let [effective-offset (calculate-effective-offset app-state node ref-shape-name mx my coord-mode)
            effective-left  (+ (d/getp shape :left) (:left effective-offset))
            effective-top   (+ (d/getp shape :top) (:top effective-offset))]
        (if (= ref-shape-name (:name shape))
          (default-position-node-shape app-state node (:name shape) mx my :offset)
          (default-position-node-shape app-state node (:name shape) effective-left effective-top :absolute))))
    (-> (e/set-bbox app-state node (merge bbox {:left (+ left mx) :top (+ top my)}))
        (layouts/do-layouts))))

(defn move-related-node [app-state node related-node relation left top]
  (let [event-data {:entity related-node
                    :relation relation
                    :app-state app-state
                    :movement-x left
                    :movement-y top}]
     (bhv/trigger-behaviour app-state related-node nil nil "moveby" event-data)))

(defn default-position-related-node [app-state node related-node relation left top]
  (move-related-node app-state node related-node relation left top))

(defn is-relation-owner [node relation]
  (= (:owner relation) (:uid node)))

(defn owned-relationships [node]
  (filterv #(is-relation-owner node %) (:relationships node)))

(defn move-node [app-state node movement-x movement-y]
  (let [shape (first (e/get-node-shape node ::c/entity-shape))
        node (e/node-by-id app-state (:uid node))]
    (default-position-node app-state
                           node
                           (:name shape)
                           movement-x
                           movement-y
                           :offset)
    (doseq [relation (owned-relationships node)]
      (let [related-node (e/node-by-id app-state (:node-id relation))]
         (default-position-related-node app-state
                                        node
                                        related-node
                                        relation
                                        movement-x
                                        movement-y)))))

(defn resize-node [app-state node width height])

(defn rotate-node [app-state node angle])

(defn refresh-bbox [app-state node use-shape-names]
  (let [node     (e/node-by-id app-state (:uid node))
        shapes   (mapv #(e/get-node-shape node %) use-shape-names)]
    (when (some? shapes)
      (let [leftmost   (apply min-key (concat [#(d/get-left %)] shapes))
            rightmost  (apply max-key (concat [#(+ (d/get-left %) (d/get-width %))] shapes))
            topmost    (apply min-key (concat [#(d/get-top %)] shapes))
            bottommost (apply max-key (concat [#(+ (d/get-top %) (d/get-height %))] shapes))]
        (-> (e/set-bbox app-state
                        node
                     {:left   (d/get-left leftmost)
                      :top    (d/get-top topmost)
                      :width  (- (+ (d/get-left rightmost) (d/get-width rightmost)) (d/get-left leftmost))
                      :height (- (+ (d/get-top bottommost) (d/get-height bottommost)) (d/get-top topmost))})
            (layouts/do-layouts))))))

(defn collides?
  ([app-state shape feature hit-callback miss-callback]
   (let [node (e/lookup app-state shape)
         collisions (filterv (fn [trg-comp]
                               (and (not= trg-comp shape)
                                    (not= (:parent-ref trg-comp) (:parent-ref shape))
                                    (d/intersects? shape trg-comp)
                                    (feature (e/lookup app-state trg-comp))))
                             (d/ordered-shapes app-state))]
        (if-let [collider (first collisions)]
          (hit-callback {:shape shape :node node}
                        {:shape collider :node (e/lookup app-state collider)})
          (miss-callback {:shape shape :node node}))))
  ([app-state shape hit-callback miss-callback]
   (collides? app-state shape #(true) hit-callback miss-callback)))

(defn includes?
  ([app-state shape feature hit-callback miss-callback])
  ([app-state shape hit-callback miss-callback]))

(defn collision-based-relations-validate
  ([app-state node]
   (let [source-node (e/node-by-id app-state (:uid node))
         source-shapes (e/shapes-of source-node)]
     (doseq [relation (:relationships source-node)]
       (let [related-node (e/node-by-id app-state (:node-id relation))
             related-shapes (e/shapes-of related-node)
             result (->> (for [component source-shapes
                               related related-shapes]
                           (d/intersects? related component))
                         (reduce #(or %1 %2) false))]
         (when-not result
           (e/disconnect-nodes app-state node related-node)))))))

(defn inclusion-based-relations-validate [app-state entity])
