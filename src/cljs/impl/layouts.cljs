(ns impl.layouts
  (:require [core.layouts :as l]))

(defn- h-start-position [context]
  (+ (l/container-left-edge context)
     (-> context :options :left)))

(defn- recalc-line-height [ebbox context]
  (js/console.log (clj->js context))
  (let [coords (:coords context)]
    (->> (if (> (:height ebbox) (:height coords))
           (:height ebbox)
           (:height coords))
         (assoc-in context [:coords :height]))))

(defn- is-new-row? [context element]
  (and (l/exceeds-container-width? context element)
       (> (-> context :coords :left) 0)))

(defn default-flow-layout [context element]
  (let [ebbox (l/bbox element)
        context  (recalc-line-height ebbox context)
        new-row? (is-new-row? context element)
        context  (if new-row?
                   (l/to-first-column (l/next-row context))
                   context)]
    (->> (l/move element context)
         (l/next-column context (:width ebbox)))))
