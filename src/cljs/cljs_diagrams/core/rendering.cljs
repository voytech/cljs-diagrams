(ns cljs-diagrams.core.rendering
  (:require [cljs-diagrams.core.eventbus :as bus]
            [cljs-diagrams.core.state :as state]))

(declare render)
(declare all-rendered)
(declare destroy-rendering-state)
(declare render-diagram)

(defn dispatch-vector [renderer-state shape & rest]
  [(:name renderer-state) (or (:rendering-method shape) (:type shape))])

(defn dispatch-scalar [renderer-state & rest]
  (:name renderer-state))

(defmulti initialize (fn [renderer dom-id width height] renderer))

(defmulti is-state-created dispatch-scalar)

(defmulti all-rendered dispatch-scalar)

(defmulti do-render dispatch-vector)

(defmulti create-rendering-state dispatch-vector)

(defmethod create-rendering-state :default [renderer-state shape])

(defmulti destroy-rendering-state dispatch-vector)

(defmethod destroy-rendering-state :default [renderer-state shape])

(defn remove-shape-rendering-state [app-state shape]
  (let [renderer-state (state/get-renderer-state app-state)]
    (destroy-rendering-state renderer-state shape)
    (state/dissoc-renderer-state app-state [:shapes (:uid shape)])))

(defn get-redraw-properties [renderer-state shape]
  (get-in renderer-state [:changes (:uid shape) :properties]))

(defn mark-for-redraw [app-state shape properties]
  (let [new-properties (concat
                        (or (-> (state/get-renderer-state app-state)
                                (get-redraw-properties shape))
                            #{})
                        properties)]
    (state/assoc-renderer-state app-state [:changes (:uid shape)] {:properties new-properties :ref shape})))

(defn mark-all-for-redraw [app-state shape]
  (mark-for-redraw app-state shape (-> shape :model keys)))

(defn rerender-shape [app-state shape]
  (-> app-state
      (remove-shape-rendering-state shape)
      (render shape)))

(defn rerender-all-properties [app-state shape]
  (-> app-state
      (remove-shape-rendering-state shape)
      (mark-all-for-redraw shape)
      (render shape)))

(defn render-node [app-state node force-all]
  (reduce #(if force-all
            (rerender-all-properties %1 %2)
            (rerender-shape %1 %2))
          app-state
          (-> (state/get-in-diagram-state app-state [:nodes (:uid node) :shapes])
              vals)))

(defn create-renderer [app-state dom-id width height renderer]
  (let [state (merge {:name renderer :shapes {}}
                     (initialize renderer dom-id width height))]
    (state/assoc-renderer-state app-state [] state)))

(defn render [app-state shape]
  (when (not (nil? shape))
    (let [renderer-state (state/get-renderer-state app-state)
          new-state (if (not (is-state-created renderer-state shape))
                      (state/assoc-renderer-state app-state
                                                  [:shapes (:uid shape) :handle]
                                                  (create-rendering-state renderer-state shape))
                      app-state)]
      (->> (get-redraw-properties renderer-state shape)
           (do-render renderer-state shape))
      (state/dissoc-renderer-state new-state [:changes (:uid shape)]))))

(defn render-changes [app-state]
  (let [shapes (-> app-state
                   (state/get-in-renderer-state [:changes])
                   vals)]
    (reduce #(render %1 (:ref %2)) app-state shapes)))

(defn render-all-properties [app-state shape]
  (when (not (nil? shape))
    (-> app-state
        (mark-all-for-redraw shape)
        (render shape))))

(defn render-diagram [app-state]
  (doseq [node (vals (state/get-in-diagram-state app-state [:nodes]))]
    (render-node app-state node true)))
