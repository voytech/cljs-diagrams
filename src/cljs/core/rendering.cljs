(ns core.rendering
  (:require [core.eventbus :as bus]))

(declare render)

(def RENDERER (atom :fabric))

(defonce rendering-context (atom {}))

(defn set-rendering [renderer]
  (reset! RENDERER renderer))

(defn get-rendering []
  @RENDERER)

(bus/on ["drawable.changed"] -999 (fn [event])
  (let [context (:context @event)
        drawable (:drawable context)]
     (swap! rendering-context assoc-in [(:property context)] (:new context))
     (render drawable)))

(defmulti do-render (fn [drawable context] [@RENDERER (:type drawable)]))

(defmulti create-rendering-state (fn [type options] [@RENDERER type]))

(defn render [drawable]
  (let [rendering-state (:rendering-state drawable)]
    (when (nil? rendering-state)
      (update-state (create-rendering-state (:type drawable))))
    (do-render drawable @rendering-context)))
