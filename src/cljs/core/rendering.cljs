(ns core.rendering
  (:require [core.eventbus :as bus]))

(declare render)

; Sets default renderers
(def RENDERER (atom :fabric))

(defonce drawables (atom {}))
; Sets default rendering options.
; Options can be :
; :auto - should rendering be triggered automatically in drawable model property  changes or only on 'rendering.execute' events ?
(def OPTIONS (atom {:auto false}))

(defonce rendering-context (atom {}))

(defn set-rendering [renderer]
  (reset! RENDERER renderer))

(defn get-rendering []
  @RENDERER)

(defn add-drawable [drawable]
  (swap! drawables assoc-in [(:type drawable) (:uid drawable)] drawable))

(defn get-drawables [type]
  (get @drawables type))

(defn get-drawable [type uid]
  (get-in @drawables [type uid]))

(defn get-drawable [uid]
  (let [merged (merge (vals @drawables))]
    (get merged uid)))

(defn remove-drawable [uid]
  (let [drawable (get-drawable uid)]
    (swap! drawables update-in [(:type drawable)] dissoc uid)))

(defn update-context [value-map]
  (reset! rendering-context (merge value-map @rendering-context)))

(defn- render-entity [entity])

(bus/on ["drawable.created"] -999 (fn [event])
  (let [context (:context @event)
        drawable (:drawable context)]
     (add-drawable drawable)))

(bus/on ["drawable.added"] -999 (fn [event]))

(bus/on ["drawable.changed"] -999 (fn [event])
  (let [context (:context @event)
        drawable (:drawable context)]
     (swap! rendering-context assoc-in [(:property context)] (:new context))
     (render drawable)))

(bus/on ["drawable.removed"] -999 (fn [event])
  (let [context (:context @event)
        drawable (:drawable context)]
     (remove-drawable drawable)
     (destroy-rendering-state drawable rendering-context)))

(bus/on ["rendering.execute"] -999 (fn [event])
 (let [context (:context @event)
       entities  (:entities context)]
    (doseq [entity enttities] (render-entity entity))))

(defmulti do-render (fn [drawable context] [@RENDERER (:type drawable)]))

(defmulti create-rendering-state (fn [drawable context] [@RENDERER (:type drawable)]))

(defmethod create-rendering-state :default [drawable context])

(defmulti destroy-rendering-state (fn [rendering-state context] [@RENDERER (:type drawable)]))

(defmethod destroy-rendering-state :default [rendering-state context])

(defn render [drawable]
  (let [rendering-state (:rendering-state drawable)]
    (when (nil? rendering-state)
      (update-state drawable (create-rendering-state drawable @rendering-context)))
    (do-render drawable @rendering-context)))
