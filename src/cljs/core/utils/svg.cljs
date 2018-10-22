(ns core.utils.svg
  (:require [core.utils.dom :as dom]))

(defn create-svg [parent-dom-id element-suffix attributes]
  (let [container (dom/by-id parent-dom-id)
        element (dom/create-node "SVG" (str parent-dom-id "-" element-suffix))]
     (dom/append-child container element)
     (dom/attrs element attributes)
     element))

(defn create-rect [svg-root attributes]
  (let [rect (dom/create-node "RECT")]
    (dom/append-child svg-root rect)
    (dom/attrs rect attributes)
    rect))

(defn create-text [svg-root attributes])

(defn create-circle [svg-root attributes])

(defn create-polygon [svg-root attributes])

(defn create-polyline [svg-root attributes])
