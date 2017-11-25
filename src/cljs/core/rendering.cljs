(ns core.rendering
  (:require [core.eventbus :as bus]
            [core.entities :as e]
            [core.components :as d]
            [core.layouts :as l]))

(declare render)
(declare all-rendered)

; Sets default renderers
(def RENDERER (atom :fabric))

; Sets default rendering options.
; Options can be :
; :auto - should rendering be triggered automatically in drawable model property  changes or only on 'rendering.execute' events ?
(def OPTIONS (atom {:auto false}))

(defonce rendering-context (volatile! {}))

(defonce drawable-states (volatile! {}))

(defn get-state-of [drawable]
  (get @drawable-states (if (record? drawable) (:uid drawable) drawable)))

(defn update-state [drawable state]
  (vswap! drawable-states assoc (if (record? drawable) (:uid drawable) drawable) state))

(defn set-rendering [renderer]
  (reset! RENDERER renderer))

(defn get-rendering []
  @RENDERER)

(defn update-context [value-map]
  (vreset! rendering-context (merge value-map @rendering-context)))

(defn clear-context [path]
  (vswap! rendering-context update-in (drop-last path) dissoc (last path)))

(defn assoc-context [path value]
  (vswap! rendering-context assoc-in path value))

(defn- render-components [components]
  (doseq [component components] (render component)))

(defn- render-entity [entity]
  (render-components  (e/components-of entity))
  (doseq [attribute-value (e/get-attributes-values entity)]
    (render-components (e/components-of attribute-value)))
  (bus/fire "rendering.finish"))

(bus/on ["rendering.context.update"] -999 (fn [event]
                                            (let [context (:context event)]
                                              (update-context context))))

(bus/on ["component.created"] -999 (fn [event]
                                    (let [context (:context event)
                                          component (:component context)]
                                        (js/console.log "component created:")
                                        (js/console.log (clj->js (merge component (d/model component)))))))


(bus/on ["component.added"] -999 (fn [event]))

(defn- update-property-to-redraw [component properties]
  (let [properties_ (concat (or (get-in @rendering-context [:redraw-properties (:uid component)]) #{}) properties)]
    (vswap! rendering-context assoc-in [:redraw-properties (:uid component)] properties_)))

(bus/on ["component.changed"] -999 (fn [event]
                                    (let [context (:context event)
                                          component (:component context)]
                                       (update-property-to-redraw component (:properties context)))))

(bus/on ["component.render" "component.layout.finished"] -999 (fn [event]
                                                                (let [context (:context event)
                                                                      component (:component context)]
                                                                   (render component))))

(bus/on ["component.removed"] -999 (fn [event]
                                    (let [context (:context event)
                                          component (:component context)]
                                       (destroy-rendering-state component @rendering-context))))

(bus/on ["entities.render"] -999 (fn [event]
                                     (let [context (:context event)
                                           entities  (:entities context)]
                                        (doseq [entity enttities] (render-entity entity)))))

(bus/on ["entity.added"] -999 (fn [event]
                                 (let [context (:context event)])))
                                    ;(render-entity (:entity context)))))

(bus/on ["entity.render"] -999 (fn [event]
                                 (let [context (:context event)]
                                    (js/console.log "entity.render fired.")
                                    (js/console.log (clj->js (:entity context)))
                                    (render-entity (:entity context)))))

(bus/on ["uncommited.render"] -999 (fn [event]
                                     (let [uncommited (get @rendering-context :redraw-properties)]
                                       (doseq [drawable-id (keys uncommited)]
                                          (render (get @d/components drawable-id))))))

(bus/on ["rendering.finish"] -999 (fn [event]
                                    (all-rendered @rendering-context)
                                    nil))

(defmulti all-rendered (fn [context] @RENDERER))

(defmulti do-render (fn [component context] [@RENDERER (:type component) (:rendering-method component)]))

(defmulti create-rendering-state (fn [component context] [@RENDERER (:type component) (:rendering-method component)]))

(defmethod create-rendering-state :default [component context])

(defmulti destroy-rendering-state (fn [component context] [@RENDERER (:type component) (:rendering-method component)]))

(defmethod destroy-rendering-state :default [rendering-state context])

(defn render [component]
  (when (not (nil? component))
    (let [rendering-state (get-state-of component)]
      (when (or (nil? rendering-state) (empty? rendering-state))
        (update-state component (create-rendering-state component @rendering-context)))
      (do-render component @rendering-context)
      (clear-context [:redraw-properties (:uid component)]))))
