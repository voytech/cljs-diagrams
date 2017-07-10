(ns core.toolctors
 (:require [core.entities :as e]))

(def DEFAULT_SIZE_OPTS {:width 100 :height 100})
(def DEFAULT_FILL {:fill "rgba(255,0,0,0.5)"})
(def DEFAULT_STROKE {:stroke "#eee" :strokeWidth 5})

;; Below is an interface to js Fabric.js library.

(defn image [data options]
  (if (not (nil? options))
    (js/fabric.Image. data (clj->js options))
    (js/fabric.Image. data)))

(defn rect [options]
  (let [enriched-opts (merge options DEFAULT_SIZE_OPTS DEFAULT_STROKE)]
    (js/fabric.Rect. (clj->js enriched-opts))))

(defn line [points options]
  (let [enriched-opts (merge options DEFAULT_SIZE_OPTS DEFAULT_STROKE)
        fabric-points (map #(js/fabric.Point. (:x %) (:y %)) points)]
    (js/fabric.Line. fabric-points (clj->js enriched-opts))))

(defn circle [options])
(defn triangle [options])
(defn ellipse [options])
(defn polyline [options])
(defn polygon [options])
(defn group [])
(defn text [data options])
(defn path [])

(defn create
  ([fabric-object data]
   (fn [context]
     (e/create-entity "" (fabric-object data context))))
  ([fabric-object]
   (fn [context]
     (e/create-entity "" (fabric-object context)))))
