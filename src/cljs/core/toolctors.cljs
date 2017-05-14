(ns core.toolctors
 (:require-macros [core.toolctors-macros :refer [pipe-toolctor]]))

(defn image [])
(defn rect [])
(defn line [])
(defn circle [])
(defn triangle [])
(defn ellipse [])
(defn polyline [])
(defn polygon [])
(defn group [])
(defn text [])
(defn path [])

(defn create [fabric-object & options]
  (pipe-toolctor context next
    (let [instance (apply fabric-object options)]
      (if (not (nil? next))
        (next instance)
        instance))))
