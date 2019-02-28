(ns cljs-diagrams.core.rendering
  (:require [cljs-diagrams.core.eventbus :as bus]
            [cljs-diagrams.core.entities :as e]
            [cljs-diagrams.core.components :as d]
            [cljs-diagrams.core.state :as state]
            [cljs-diagrams.core.layouts :as l]))

(declare render)
(declare all-rendered)
(declare destroy-rendering-state)

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
  (mark-for-redraw app-state component (keys (d/model component))))

(defn get-redraw-properties [renderer-state component]
  (get-in renderer-state [:components (:uid component) :redraw-properties]))

(defn render-entity [app-state entity force-all]
  (l/do-layouts entity)
  (doseq [component (e/components-of entity)]
    (when force-all (mark-all-for-redraw app-state component))
    (render app-state component)))


(defn register-handlers [app-state]
  (bus/on app-state ["component.created"] -999 (fn [event]
                                                (let [{:keys [component app-state]} (:context event)])))

  (bus/on app-state ["component.changed"] -999 (fn [event]
                                                (let [{:keys [component app-state properties]} (:context event)]
                                                   (mark-for-redraw app-state component properties))))

  (bus/on app-state ["component.added"] -999 (fn [event]
                                              (let [{:keys [component app-state]} (:context event)]
                                                 (mark-all-for-redraw app-state component))))

  (bus/on app-state ["component.render" "component.layout.finished"] -999 (fn [event]
                                                                            (let [{:keys [component app-state]} (:context event)]
                                                                               (render app-state component))))

  (bus/on app-state ["component.removed"] -999 (fn [event]
                                                 (let [{:keys [component app-state]} (:context event)]
                                                    (remove-component app-state component))))

  (bus/on app-state ["entities.render"] -999 (fn [event]
                                               (let [{:keys [app-state]} (:context event)
                                                     entities (vals (state/get-in-diagram-state app-state [:entities]))]
                                                  (doseq [entity entities]
                                                    (render-entity app-state entity)))))

  (bus/on app-state ["entity.added"] -999 (fn [event]
                                            (let [context (:context event)])))

  (bus/on app-state ["entity.render"] -999 (fn [event]
                                             (let [{:keys [entity app-state]} (:context event)]
                                                (render-entity app-state entity))))

  (bus/on app-state ["uncommited.render"] -999 (fn [event]
                                                 (let [app-state (-> event :context :app-state)
                                                       components (-> (state/get-in-renderer-state app-state [:components])
                                                                      vals)]
                                                   (doseq [component components]
                                                      (render app-state (:ref component))))))

  (bus/on app-state ["rendering.finish"] -999 (fn [event]
                                                (let [app-state (-> event :context :app-state)]
                                                  (all-rendered (state/get-renderer-state app-state))
                                                  nil))))

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

(defn create-renderer [app-state dom-id width height renderer]
  (register-handlers app-state)
  (let [state (merge {:name renderer :components {}}
                     (initialize renderer dom-id width height))]
    (state/assoc-renderer-state app-state [] state)))

(defn render [app-state component]
  (when (not (nil? component))
    (let [renderer-state (state/get-renderer-state app-state)]
      (when (not (is-state-created renderer-state component))
         (create-rendering-state renderer-state component))
      (->> (get-redraw-properties renderer-state component)
           (do-render renderer-state component))
      (state/dissoc-renderer-state app-state [:components (:uid component) :redraw-properties]))))

(defn render-all-properties [app-state component]
  (when (not (nil? component))
    (mark-all-for-redraw app-state component)
    (render app-state component)))

(defn render-diagram [app-state]
  (doseq [entity (vals (state/get-in-diagram-state app-state [:entities]))]
    (mark-all-components-for-redraw app-state entity)
    (render-entity app-state entity true)))
