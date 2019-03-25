(ns cljs-diagrams.impl.layouts.flow
  (:require [cljs-diagrams.core.layouts :as l]
            [clojure.spec.alpha :as spec]
            [cljs-diagrams.core.shapes :as c]))

(spec/def ::hints (spec/keys :req-un [::padding]))

(defn next-column [context offset]
   (assoc-in context [:coords :left] (+ (-> context :coords :left) offset)))

(defn next-row [context]
   (merge context {:coords {:top (+ (-> context :coords :top)
                                    (-> context :coords :height))
                            :left 0
                            :height 0}}))

(defn to-first-column [context bbox]
  (assoc-in context [:coords :left] (:left bbox)))

(defn reset [context path value]
  (assoc-in context path value))

(defn reset-line-height [context]
  (reset context [:coords :height] 0))

(defn layout-left-edge [context]
  (-> context :orig-pos :left))

(defn layout-right-edge [context]
  (let [left (-> context :orig-pos :left)
        width (-> context :orig-size :width)]
    (+ left width)))

(defn layout-top-edge [context]
  (-> context :orig-pos :top))

(defn layout-bottom-edge [context]
  (let [top (-> context :orig-pos :top)
        height (-> context :orig-size :height)]
    (+ top height)))

(defn absolute-top [context]
  (+ (layout-top-edge context) (-> context :coords :top)))

(defn absolute-left [context]
  (+ (layout-left-edge context) (-> context :coords :left)))

(defn exceeds-container-width? [context shapes]
  (> (+ (absolute-left context) (c/get-width shapes)) (layout-right-edge context)))

(defn exceeds-container-height? [context shapes]
  (> (+ (absolute-top context) (c/get-height shapes)) (layout-bottom-edge context)))

(defn- recalc-line-height [context cbbox]
  (let [coords (:coords context)]
    (->> (if (> (:height cbbox) (:height coords))
           (:height cbbox)
           (:height coords))
         (assoc-in context [:coords :height]))))

(defn- is-new-row? [context shapes]
  (and (exceeds-container-width? context shapes)
       (> (-> context :coords :left) 0)))

(defmethod l/create-context ::flow [app-state node layout]
  {:orig-pos  (l/absolute-position-of-layout node layout)
   :orig-size (l/absolute-size-of-layout node layout)
   :coords {:top 0
            :left 0
            :height 0}})

(defmethod l/layout-function ::flow [node shape context]
  (let [cbbox (c/get-bbox shape)
        padding (-> shape :layout-attributes :layout-hints :padding)
        context  (recalc-line-height context cbbox)
        new-row? (is-new-row? context shape)
        context  (if new-row? (next-row context) context)]
    {:to-set {
         :left (absolute-left context)
         :top (absolute-top context)}
     :processing-context (next-column context (+ padding (:width cbbox)))}))

(defn flow-layout-attributes [name idx padding]
  (c/layout-attributes name idx {:padding padding}))
