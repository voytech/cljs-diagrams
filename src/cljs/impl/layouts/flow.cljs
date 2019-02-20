(ns impl.layouts.flow
  (:require [core.layouts :as l]
            [core.components :as c]))

(defn next-column [context offset]
   (assoc-in context [:coords :left] (+ (-> context :coords :left) offset)))

(defn next-row [context]
   (merge context {:coords {:top (+ (-> context :coords :top)
                                    (-> context :coords :height))
                            :left (or (-> context :options :left) 0)
                            :height 0}}))

(defn to-first-column [context bbox]
  (assoc-in context [:coords :left] (:left bbox)))

(defn to-first-row [cotnext bbox]
  (assoc-in context [:coords :top] (:top bbox)))

(defn reset [context path value]
  (assoc-in context path value))

(defn reset-line-height [context]
  (reset context [:coords :height] 0))

(defn absolute-top [context bbox]
  (+ (:top bbox) (-> context :coords :top)))

(defn absolute-left [context bbox]
  (+ (:left bbox) (-> context :coords :left)))

(defn container-left-edge [bbox]
  (:left bbox))

(defn container-right-edge [bbox]
  (let [{:keys [left width]} bbox]
    (+ left width)))

(defn container-top-edge [bbox]
  (:top bbox))

(defn container-bottom-edge [bbox]
  (let [{:keys [top height]} bbox]
    (+ top height)))

(defn move [element context]
  (let [left (absolute-left context)
        top  (absolute-top context)]
    (move-element element left top)
    context))

(defn exceeds-container-width? [context element bbox]
  (> (+ (absolute-left context bbox) (c/get-width element)) (container-right-edge bbox)))

(defn exceeds-container-height? [context element bbox]
  (> (+ (absolute-top context bbox) (c/get-height element)) (container-bottom-edge bbox)))

(defn- recalc-line-height [context ebbox]
  (let [coords (:coords context)]
    (->> (if (> (:height ebbox) (:height coords))
           (:height ebbox)
           (:height coords))
         (assoc-in context [:coords :height]))))

(defn- is-new-row? [context element bbox]
  (and (l/exceeds-container-width? context element bbox)
       (> (-> context :coords :left) 0)))

(defmethod l/create-context ::flow [layout]
  {:coords {:top 0
            :left 0
            :height 0}})

(defn flow-layout [entity component context]
  (let [ebbox (:bbox entity)
        context  (recalc-line-height context ebbox)
        new-row? (is-new-row? context component ebbox)
        context  (if new-row?
                   (to-first-column (next-row context) ebbox)
                   context)]
    (->> (l/move component context)
         (next-column context (:width ebbox)))))
