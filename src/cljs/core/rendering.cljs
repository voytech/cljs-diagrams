(ns core.rendering
  (:require [core.eventbus :as bus]))

(def RENDERER (atom :fabric))

(defn set-rendering [renderer]
  (reset! RENDERER renderer))

(defn get-rendering []
  @RENDERER)

(defmulti do-render (fn [drawable] [@RENDERER (:type drawable)]))

(defmulti create-rendering-state (fn [type options] [@RENDERER type]))

(defn render [drawable]
  (let [rendering-state (:rendering-state drawable)]
    (when (nil? rendering-state)
      (update-state (create-rendering-state (:type drawable))))
    (do-render drawable)))
