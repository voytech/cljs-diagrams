(ns impl.layouts
  (:require [core.layouts :as l]))

(defn default-flow-layout [context element]
  (js/console.log "default flow layout input context")
  (js/console.log (clj->js context))
  (let [element-bbox (l/bbox element)
        coords (:coords context)
        context (assoc-in  context [:coords :height] (if (> (:height element-bbox) (:height coords))
                                                         (:height element-bbox)
                                                         (:height coords)))
        new-row? (and (l/exceeds-container-width? context element) (> (:left coords) 0))
        elements-coords (if new-row?
                           {:left (l/container-left-edge context) :top (l/absolute-next-row context)}
                           {:left (l/absolute-row-left context)   :top (l/absolute-row-top context)})]
    (js/console.log "Layout Resolve Coordinates-------->")
    (js/console.log (clj->js context))
    (js/console.log (clj->js new-row?))
    (js/console.log (clj->js elements-coords))
    (js/console.log "Layout Resolve Coordinates--------<")
    (l/move-element element (:left elements-coords) (:top elements-coords))
    (l/alter context (if new-row? (l/absolute-next-row context) 0) (if new-row?  0  (:width element-bbox)))))
