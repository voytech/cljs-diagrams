(ns core.rendering
  (:require [core.eventbus :as bus]))

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

(defn- render-entity [entity])

(bus/on ["drawable.changed"] -999 (fn [event])
  (let [context (:context @event)
        drawable (:drawable context)]
     (swap! rendering-context assoc-in [(:property context)] (:new context))
     (render drawable)))

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
      (update-state (create-rendering-state (:type drawable))))
    (do-render drawable @rendering-context)))
