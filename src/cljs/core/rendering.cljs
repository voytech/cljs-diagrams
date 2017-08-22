(ns core.rendering
  (:require [core.eventbus :as bus])
  (:require [core.layouts :as l]))

(declare render)

; Sets default renderers
(def RENDERER (atom :fabric))

; Sets default rendering options.
; Options can be :
; :auto - should rendering be triggered automatically in drawable model property  changes or only on 'rendering.execute' events ?
(def OPTIONS (atom {:auto false}))

(defonce rendering-context (atom {}))

(defn set-rendering [renderer]
  (reset! RENDERER renderer))

(defn get-rendering []
  @RENDERER)

(defn update-context [value-map]
  (reset! rendering-context (merge value-map @rendering-context)))

(defn clear-context [path]
  (swap! rendering-context update-in (drop-last path) dissoc (last path)))

(defn assoc-context [path value]
  (swap! rendering-context assoc-in path value))

(defn- render-components [components]
  (doseq [component components]
     (let [drawable (:drawable component)]
       (render drawable))))

(defn- render-entity [entity]
  (render-components  (e/components entity))
  (doseq [attribute-value (:attributes entity)]
    (render-components (e/components attribute-value)))
  (let [bbox (layouts/get-bbox entity)]
    (l/layout bbox (:attributes entity))))

(bus/on ["rendering.context.update" -999 (fn [event]
                                            (let [context (:context @event)]
                                              (update-context context)))])

(bus/on ["drawable.created"] -999 (fn [event]
                                    (let [context (:context @event)
                                          drawable (:drawable context)])))


(bus/on ["drawable.added"] -999 (fn [event]))

(defn- update-property-to-redraw [drawable property newvalue oldvalue]
  (swap! rendering-context assoc-in [:redraw-properties (uid drawable) property] {:new newvalue :old oldvalue}))

(bus/on ["drawable.changed"] -999 (fn [event]
                                    (let [context (:context @event)
                                          drawable (:drawable context)]
                                       (update-property-to-redraw drawable (:property context) (:new context) (:old context))
                                       (render drawable))))

(bus/on ["drawable.removed"] -999 (fn [event]
                                    (let [context (:context @event)
                                          drawable (:drawable context)]
                                       (destroy-rendering-state drawable rendering-context))))

(bus/on ["entities.render"] -999 (fn [event]
                                     (let [context (:context @event)
                                           entities  (:entities context)]
                                        (doseq [entity enttities] (render-entity entity)))))

(bus/on ["entity.added"] -999 (fn [event]
                                 (let [context (:context @event)]
                                    (render-entity (:entity context)))))

(bus/on ["entity.render"] -999 (fn [event]
                                 (let [context (:context @event)]
                                    (render-entity (:entity context)))))

(defmulti do-render (fn [drawable context] [@RENDERER (:type drawable)]))

(defmulti create-rendering-state (fn [drawable context] [@RENDERER (:type drawable)]))

(defmethod create-rendering-state :default [drawable context])

(defmulti destroy-rendering-state (fn [drawable context] [@RENDERER (:type drawable)]))

(defmethod destroy-rendering-state :default [rendering-state context])

(defn render [drawable]
  (let [rendering-state (d/state drawable)]
    (when (nil? rendering-state)
      (update-state drawable (create-rendering-state drawable @rendering-context)))
    (do-render drawable @rendering-context)))
