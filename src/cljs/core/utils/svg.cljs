(ns core.utils.svg
  (:require [core.utils.dom :as dom]))

(defn create-svg [parent-dom-id element-suffix attributes]
  (let [container (dom/by-id parent-dom-id)
        element (dom/create-ns-node "http://www.w3.org/2000/svg" "svg" (str parent-dom-id "-" element-suffix))]
     (dom/append-child container element)
     (dom/attrs element attributes)
     element))

(defn create-element [type svg-root attributes]
 (let [rect (dom/create-ns-node "http://www.w3.org/2000/svg" type)]
   (dom/attrs rect attributes)
   (dom/append-child svg-root rect)
   rect))

(defn create-rect [svg-root attributes]
  (create-element "rect" svg-root attributes))

(defn create-text [svg-root attributes]
  (create-element "text" svg-root attributes))

(defn create-circle [svg-root attributes]
  (create-element "circle" svg-root attributes))

(defn create-image [svg-root attributes]
  (create-element "image" scg-root attributes))

(defn create-line [svg-root attributes]
  (create-element "line" scg-root attributes))

(defn create-path [svg-root attributes]
  (create-element "path" scg-root attributes))
