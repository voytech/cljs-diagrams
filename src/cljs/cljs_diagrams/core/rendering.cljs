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

(defn remove-shape [app-state shape]
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
  (mark-for-redraw app-state shape (-> shape :model deref keys)))

(defn render-node [app-state node force-all]
  (doseq [shape (-> node :shapes vals)
          app-state (remove-shape app-state shape)]
    (-> (if force-all (mark-all-for-redraw app-state shape) app-state)
        (render shape))))

(defn create-renderer [app-state dom-id width height renderer]
  (let [state (merge {:name renderer :shapes {}}
                     (initialize renderer dom-id width height))]
    (state/assoc-renderer-state app-state [] state)))

(defn render [app-state shape]
  (when (not (nil? shape))
    (let [renderer-state (state/get-renderer-state app-state)]
      (when (not (is-state-created renderer-state shape))
        (state/assoc-renderer-state app-state
                                    [:shapes (:uid shape) :handle]
                                    (create-rendering-state renderer-state shape)))
      (->> (get-redraw-properties renderer-state shape)
           (do-render renderer-state shape))
      (state/dissoc-renderer-state app-state [:changes (:uid shape)]))))

(defn render-changes [app-state]
  (let [shapes (-> app-state
                       (state/get-in-renderer-state [:changes])
                       vals)]
    (doseq [shape shapes]
      (render app-state (:ref shape)))))

(defn render-all-properties [app-state shape]
  (when (not (nil? shape))
    (mark-all-for-redraw app-state shape)
    (render app-state shape)))

(defn render-diagram [app-state]
  (doseq [node (vals (state/get-in-diagram-state app-state [:nodes]))]
    (render-node app-state node true)))
