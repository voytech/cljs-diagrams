(ns core.rendering
  (:require [core.eventbus :as bus]
            [core.entities :as e]
            [core.drawables :as d]
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
  (doseq [component components]
     (let [drawable (:drawable component)]
       (render drawable))))

(defn- render-entity [entity]
  (render-components  (e/components entity))
  (doseq [attribute-value (:attributes entity)]
    (render-components (e/components attribute-value)))
  (let [bbox (l/get-bbox entity)
        cbox {:left (+ (:left (e/get-entity-content-bbox entity)) (:left bbox))
              :top  (+ (:top (e/get-entity-content-bbox entity)) (:top bbox))
              :width (:width (e/get-entity-content-bbox entity))
              :height (:height (e/get-entity-content-bbox entity))}]
    (l/layout cbox (e/get-attributes-values entity))
    (bus/fire "rendering.finish")))

(bus/on ["rendering.context.update"] -999 (fn [event]
                                            (let [context (:context @event)]
                                              (js/console.log "rendering.context.update handled")
                                              (update-context context)
                                              (js/console.log (clj->js @rendering-context)))))

(bus/on ["drawable.created"] -999 (fn [event]
                                    (let [context (:context @event)
                                          drawable (:drawable context)])))


(bus/on ["drawable.added"] -999 (fn [event]))

(defn- update-property-to-redraw [drawable properties]
  (let [properties_ (concat (or (get-in @rendering-context [:redraw-properties (:uid drawable)]) #{}) properties)]
    (vswap! rendering-context assoc-in [:redraw-properties (:uid drawable)] properties_)))

(bus/on ["drawable.changed"] -999 (fn [event]
                                    (let [context (:context @event)
                                          drawable (:drawable context)]
                                       (update-property-to-redraw drawable (:properties context)))))

(bus/on ["drawable.render" "drawable.layout.finished"] -999 (fn [event]
                                                              (let [context (:context @event)
                                                                    drawable (:drawable context)]
                                                                 (render drawable))))

(bus/on ["drawable.removed"] -999 (fn [event]
                                    (let [context (:context @event)
                                          drawable (:drawable context)]
                                       (destroy-rendering-state drawable @rendering-context))))

(bus/on ["entities.render"] -999 (fn [event]
                                     (let [context (:context @event)
                                           entities  (:entities context)]
                                        (doseq [entity enttities] (render-entity entity)))))

(bus/on ["entity.added"] -999 (fn [event]
                                 (let [context (:context @event)])))
                                    ;(render-entity (:entity context)))))

(bus/on ["entity.render"] -999 (fn [event]
                                 (let [context (:context @event)]
                                    (js/console.log "entity.render fired.")
                                    (js/console.log (clj->js (:entity context)))
                                    (render-entity (:entity context)))))

(bus/on ["uncommited.render"] -999 (fn [event]
                                     (let [uncommited (get @rendering-context :redraw-properties)]
                                       (doseq [drawable-id (keys uncommited)]
                                          (render (get @d/drawables drawable-id))))))

(bus/on ["rendering.finish"] -999 (fn [event]
                                    (all-rendered @rendering-context)
                                    nil))

(defmulti all-rendered (fn [context] @RENDERER))

(defmulti do-render (fn [drawable context] [@RENDERER (:type drawable)]))

(defmulti create-rendering-state (fn [drawable context] [@RENDERER (:type drawable)]))

(defmethod create-rendering-state :default [drawable context])

(defmulti destroy-rendering-state (fn [drawable context] [@RENDERER (:type drawable)]))

(defmethod destroy-rendering-state :default [rendering-state context])

(defn render [drawable]
  (when (not (nil? drawable))
    (let [rendering-state (d/state drawable)]
      (when (or (nil? rendering-state) (empty? rendering-state))
        (d/update-state drawable (create-rendering-state drawable @rendering-context)))
      (do-render drawable @rendering-context)
      (clear-context [:redraw-properties (:uid drawable)]))))
