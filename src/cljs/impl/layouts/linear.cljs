(ns impl.layouts.linear
  (:require [core.layouts :as l]))

(defmethod l/create-context [layout]
  {:coords {:top 0
            :left 0
            :height 0}})

(defn create-evaluation-context [entity opts]
  {:container-bbox (:bbox entity)
   :options opts
   :coords {:top (or (:top options) 0)
            :left (or (:left options) 0)
            :height 0}}))

(defn next-column [context offset]
   (assoc-in context [:coords :left] (+ (-> context :coords :left) offset)))

(defn next-row [context]
   (merge context {:coords {:top (+ (-> context :coords :top)
                                    (-> context :coords :height))
                            :left (or (-> context :options :left) 0)
                            :height 0}}))

(defn to-first-column [context]
  (assoc-in context [:coords :left] (-> context :options :left)))

(defn to-first-row [cotnext]
  (assoc-in context [:coords :top] (-> context :options :top)))

(defn reset [context path value]
  (assoc-in context path value))

(defn reset-line-height [context]
  (reset context [:coords :height] 0))

(defn absolute-top [context]
  (+ (-> context :container-bbox :top) (-> context :coords :top)))

(defn absolute-left [context]
  (+ (-> context :container-bbox :left) (-> context :coords :left)))

(defn container-left-edge [context]
  (-> context :container-bbox :left))

(defn container-right-edge [context]
  (let [{:keys [left width]} (:container-bbox context)]
    (+ left width)))

(defn container-top-edge [context]
  (-> context :container-bbox :top))

(defn container-bottom-edge [context]
  (let [{:keys [top height]} (:container-bbox context)]
    (+ top height)))

(defn move [element context]
  (let [left (absolute-left context)
        top  (absolute-top context)]
    (move-element element left top)
    context))

(defn exceeds-container-width? [context element]
  (> (+ (absolute-left context) (:width (bbox element))) (container-right-edge context)))

(defn exceeds-container-height? [context element]
  (> (+ (absolute-top context) (:height (bbox element))) (container-bottom-edge context)))

(defn- recalc-line-height [context ebbox]
  (let [coords (:coords context)]
    (->> (if (> (:height ebbox) (:height coords))
           (:height ebbox)
           (:height coords))
         (assoc-in context [:coords :height]))))

(defn- is-new-row? [context element]
  (and (l/exceeds-container-width? context element)
       (> (-> context :coords :left) 0)))

(defn linear-layout [context component]
  (let [ebbox (l/bbox element)
        context  (recalc-line-height context ebbox)
        new-row? (is-new-row? context element)
        context  (if new-row?
                   (l/to-first-column (l/next-row context))
                   context)]
    (->> (l/move element context)
         (l/next-column context (:width ebbox)))))
