(ns core.rendering
  (:require [core.eventbus :as bus]
            [core.entities :as e]
            [core.components :as d]
            [core.layouts :as l]))

(declare render)
(declare all-rendered)
(declare destroy-rendering-state)

(defn renderer-name [renderer-state]
  (-> renderer-state deref :name))

(defn- render-components [renderer-state components]
  (doseq [component components]
    (render renderer-state component)))

(defn- render-entity [renderer-state entity]
  (l/do-layouts entity)
  (render-components renderer-state (e/components-of entity))
  (bus/fire "rendering.finish"))

(defn- update-property-to-redraw [renderer-state component properties]
  (let [new-properties (concat
                          (or (get-in @renderer-state [:components (:uid component) :redraw-properties]) #{})
                          properties)]
    (swap! renderer-state assoc-in [:components (:uid component)] {:redraw-properties new-properties :ref component})))

(bus/on ["component.created"] -999 (fn [event]
                                    (let [{:keys [component app-state]} (:context event)])))

(bus/on ["component.changed"] -999 (fn [event]
                                    (let [{:keys [component app-state properties]} (:context event)
                                          renderer-state (-> app-state deref :renderer)]
                                       (update-property-to-redraw renderer-state component properties))))

(bus/on ["component.added"] -999 (fn [event]
                                  (let [{:keys [component app-state]} (:context event)
                                        renderer-state (-> app-state deref :renderer)]
                                     (update-property-to-redraw renderer-state component (keys (d/model component))))))

(bus/on ["component.render" "component.layout.finished"] -999 (fn [event]
                                                                (let [{:keys [component app-state]} (:context event)]
                                                                   (render app-state component))))

(bus/on ["component.removed"] -999 (fn [event]
                                    (let [{:keys [component app-state]} (:context event)
                                          renderer-state (-> app-state deref :renderer)]
                                       (destroy-rendering-state renderer-state component))))

(bus/on ["entities.render"] -999 (fn [event]
                                     (let [{:keys [app-state]} (:context event)
                                           entities (-> app-state deref :entities deref vals)]
                                        (doseq [entity entities]
                                          (render-entity app-state entity)))))

(bus/on ["entity.added"] -999 (fn [event]
                                 (let [context (:context event)])))

(bus/on ["entity.render"] -999 (fn [event]
                                 (let [{:keys [entity app-state]} (:context event)
                                        renderer-state (-> app-state deref :renderer)]
                                    (render-entity renderer-state entity))))

(bus/on ["uncommited.render"] -999 (fn [event]
                                     (let [app-state (-> event :context :app-state)
                                           renderer-state (-> app-state deref :renderer)
                                           components (-> @renderer-state :components vals)]
                                       (doseq [component components]
                                          (render renderer-state (:ref component))))))

(bus/on ["rendering.finish"] -999 (fn [event]
                                    (all-rendered (-> event
                                                      :context
                                                      :app-state
                                                      deref
                                                      :renderer))
                                    nil))

(defmulti initialize (fn [renderer app-state dom-id width height initial-state] renderer))

(defmulti all-rendered (fn [renderer-state] (renderer-name renderer-state)))

(defmulti do-render (fn [renderer-state component] [(renderer-name renderer-state) (or (:rendering-method component) (:type component))]))

(defmulti create-rendering-state (fn [renderer-state component] [(renderer-name renderer-state) (or (:rendering-method component) (:type component))]))

(defmethod create-rendering-state :default [renderer-state component])

(defmulti destroy-rendering-state (fn [renderer-state component] [(renderer-name renderer-state) (or (:rendering-method component) (:type component))]))

(defmethod destroy-rendering-state :default [renderer-state component])

(defn create-renderer [app-state dom-id width height renderer]
  (let [initial-state {:name renderer :components {}}]
    (swap! app-state assoc :renderer
      (initialize renderer app-state dom-id width height initial-state))))

(defn render [renderer-state component]
  (when (not (nil? component))
    (let [component-state (-> renderer-state deref :components (:uid component))]
      (when (or (nil? component-state)
                (empty? component-state)
                (empty? (:dom component-state)))
        (swap! renderer-state assoc-in
          [:components (:uid component)]
          (create-rendering-state renderer-state component)))
      (do-render renderer-state component)
      (swap! renderer-state update-in
        [:components (:uid component)] dissoc :redraw-properties))))
