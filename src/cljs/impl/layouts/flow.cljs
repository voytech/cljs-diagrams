(ns impl.layouts.flow
  (:require [core.layouts :as l]
            [core.components :as c]))

(defrecord LayoutHints [padding])

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

(defn move [component context]
  (let [left (absolute-left context)
        top  (absolute-top context)]
    (c/set-data component {:left left :top top})
    context))

(defn exceeds-container-width? [context component]
  (> (+ (absolute-left context) (c/get-width component)) (layout-right-edge context)))

(defn exceeds-container-height? [context component]
  (> (+ (absolute-top context) (c/get-height component)) (layout-bottom-edge context)))

(defn- recalc-line-height [context cbbox]
  (let [coords (:coords context)]
    (->> (if (> (:height cbbox) (:height coords))
           (:height cbbox)
           (:height coords))
         (assoc-in context [:coords :height]))))

(defn- is-new-row? [context component]
  (and (exceeds-container-width? context component)
       (> (-> context :coords :left) 0)))

(defmethod l/create-context ::flow [entity layout]
  {:orig-pos  (l/absolute-position-of-layout entity layout)
   :orig-size (l/absolute-size-of-layout entity layout)
   :coords {:top 0
            :left 0
            :height 0}})

(defn flow-layout [entity component context]
  (let [cbbox (c/get-bbox component)
        padding (-> component :layout-attributes :layout-hints :padding)
        context  (recalc-line-height context cbbox)
        new-row? (is-new-row? context component)
        context  (if new-row? (next-row context) context)]
    (-> (move component context)
        (next-column (+ padding (:width cbbox))))))

(defn flow-layout-attributes [idx padding]
  (c/layout-attributes ::flow idx (LayoutHints. padding)))
