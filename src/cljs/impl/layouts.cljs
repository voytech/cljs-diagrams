(ns impl.layouts
  (:require [core.layouts :as l]))

(defn default-flow-layout [context element]
  (let [element-bbox (l/bbox element)
        context (assoc context :current-row-height (if (> (:height element-bbox) (:current-row-height context))
                                                       (:height element-bbox)
                                                       (:current-row-height context)))
        new-row? (and (l/exceeds-container-width? context element) (> (:current-row-left context) 0))
        element-coords (if new-row?
                           {:left (l/container-left-edge context) :top (l/absolute-next-row context)}
                           {:left (l/absolute-row-left context)   :top (l/absolute-row-top context)})]
    (l/move-element element (:left elements-coords) (:top elements-coords))
    (l/alter context (if new-row? (l/absolute-next-row context) 0)
                   (if new-row?  0  (:width element-bbox)))))

(defn generic-layout [context element])
