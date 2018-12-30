(ns impl.layouts
  (:require [core.layouts :as l]))

(defn- recalc-line-height [context ebbox]
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
        context  (recalc-line-height context ebbox)
        new-row? (is-new-row? context element)
        context  (if new-row?
                   (l/to-first-column (l/next-row context))
                   context)]
    (->> (l/move element context)
         (l/next-column context (:width ebbox)))))

(defn relative-layout [context element]
  (when-let [{:keys [rel-position
                     rel-size
                     rel-origin] :as hints} (:layout-hints element)]
                     
    ))
