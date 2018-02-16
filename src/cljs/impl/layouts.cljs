(ns impl.layouts
  (:require [core.layouts :as l]))

(defn- h-start-position [context]
  (+ (l/container-left-edge context)
     (-> context :options :left)))

(defn- recalc-lein-height [ebbox context]
  (let [coords (:coords context)]
    (->> (if (> (:height ebbox) (:height coords))
           (:height ebbox)
           (:height coords))
         (assoc-in context [:coords :height]))))

(defn- reset-line-height [context]
  (assoc-in context [:coords :height] 0))

(defn- is-new-row? [context element]
  (and (l/exceeds-container-width? context element)
       (> (-> context :coords :left) 0)))

(defn default-flow-layout [context element]
  (let [ebbox (l/bbox element)
        context  (recalc-lein-height ebbox context)
        new-row? (is-new-row? context element)
        context  (if new-row?
                   (l/to-first-column (l/next-row context)))
                   ;(l/next-column context (:width ebbox)))
        elements-coords (if new-row?
                           {:left (h-start-position context)    :top (l/absolute-next-row context)}
                           {:left (l/absolute-row-left context) :top (l/absolute-row-top context)})]
    (l/move-element element (:left elements-coords) (:top elements-coords))))
