(ns core.toolctors
 (:require [core.entities :as e])
 (:require-macros [core.toolctors-macros :refer [pipe-toolctor]]))

(defn image [data options]
  (if (not (nil? options))
    (js/fabric.Image. data (clj->js options))
    (js/fabric.Image. data)))

;; Below is an interface to js Fabric.js library.
(defn rect [options])
(defn line [options])
(defn circle [options])
(defn triangle [options])
(defn ellipse [options])
(defn polyline [options])
(defn polygon [options])
(defn group [])
(defn text [data options])
(defn path [])

(defn next-in-chain [context next]
  (if (not (nil? next))
    (next context)
    context))

(defn create [fabric-object data]
  (pipe-toolctor context next
    (let [instance (apply fabric-object [data context])
          entity (e/create-entity "" instance [])]
      (next-in-chain entity next))))

(defn set-properties [property-map]
  (pipe-toolctor context next
    (let [entity context
          src (:src entity)]
      (for [key (keys property-map)]
        (goog.object/set src (name key) (key property-map)))
      (refresh entity)
      (next-in-chain entity next))))
