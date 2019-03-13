(ns cljs-diagrams.core.rendering
  (:require [cljs-diagrams.core.eventbus :as bus]
            [cljs-diagrams.core.state :as state]))

(declare render)
(declare all-rendered)
(declare destroy-rendering-state)
(declare render-diagram)

(defn dispatch-vector [renderer-state component & rest]
  [(:name renderer-state) (or (:rendering-method component) (:type component))])

(defn dispatch-scalar [renderer-state & rest]
  (:name renderer-state))

(defmulti initialize (fn [renderer dom-id width height] renderer))

(defmulti is-state-created dispatch-scalar)

(defmulti all-rendered dispatch-scalar)

(defmulti do-render dispatch-vector)

(defmulti create-rendering-state dispatch-vector)

(defmethod create-rendering-state :default [renderer-state component])

(defmulti destroy-rendering-state dispatch-vector)

(defmethod destroy-rendering-state :default [renderer-state component])


(defn remove-component [app-state component]
  (let [renderer-state (state/get-renderer-state app-state)]
    (destroy-rendering-state renderer-state component)
    (state/dissoc-renderer-state app-state [:components (:uid component)])))

(defn mark-for-redraw [app-state component properties]
  (let [new-properties (concat
                          (or (state/get-in-renderer-state app-state [:components (:uid component) :redraw-properties])
                              #{})
                          properties)]
    (state/assoc-renderer-state app-state [:components (:uid component)] {:redraw-properties new-properties :ref component})
    new-properties))

(defn mark-all-for-redraw [app-state component]
  (mark-for-redraw app-state component (-> component :model deref keys)))

(defn get-redraw-properties [renderer-state component]
  (get-in renderer-state [:components (:uid component) :redraw-properties]))

(defn render-entity [app-state entity force-all]
  (doseq [component (-> entity :components vals)]
    (remove-component app-state component)
    (when force-all (mark-all-for-redraw app-state component))
    (render app-state component)))

(defn create-renderer [app-state dom-id width height renderer]
  (let [state (merge {:name renderer :components {}}
                     (initialize renderer dom-id width height))]
    (state/assoc-renderer-state app-state [] state)))

(defn render [app-state component]
  (when (not (nil? component))
    (let [renderer-state (state/get-renderer-state app-state)]
      (when (not (is-state-created renderer-state component))
        (state/assoc-renderer-state app-state
                                    [:components (:uid component) :handle]
                                    (create-rendering-state renderer-state component)))
      (->> (get-redraw-properties renderer-state component)
           (do-render renderer-state component))
      (state/dissoc-renderer-state app-state [:components (:uid component) :redraw-properties]))))

(defn render-changes [app-state]
  (let [components (-> app-state
                       (state/get-in-renderer-state [:components])
                       vals)]
    (doseq [component components]
      (render app-state (:ref component)))))

(defn render-all-properties [app-state component]
  (when (not (nil? component))
    (mark-all-for-redraw app-state component)
    (render app-state component)))

(defn render-diagram [app-state]
  (doseq [entity (vals (state/get-in-diagram-state app-state [:entities]))]
    (render-entity app-state entity true)))
