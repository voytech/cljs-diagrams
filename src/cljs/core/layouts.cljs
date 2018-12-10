(ns core.layouts
  (:require [core.components :as d]
            [core.eventbus :as b]))

(defrecord Layout [layout-func select-func options])

(defn get-components [container]
  (if (not (nil? (:components container)))
    (if (map? (:components container))
      (vals (:components container))
      (:components container))
    (throw (js/Error. "Parameter passed to get-components is supposed to be component container but IS NOT !"))))

(defn get-bbox [container]
  (when (> (count (get-components container)) 0)
    (let [sources (get-components container)
          leftmost   (apply min-key (conj sources #(d/get-left %)))
          rightmost  (apply max-key (conj sources #(+ (d/get-left %) (d/get-width %))))
          topmost    (apply min-key (conj sources #(d/get-top %)))
          bottommost (apply max-key (conj sources #(+ (d/get-top %) (d/get-height %))))]
      {:left (d/get-left leftmost)
       :top  (d/get-top topmost)
       :width (- (+ (d/get-left rightmost) (d/get-width rightmost)) (d/get-left leftmost))
       :height (- (+ (d/get-top bottommost) (d/get-height  bottommost)) (d/get-top topmost))})))

; Interface function for adding new elements into specific containers using layout.

(defn- is-container [element]
  (not (nil? (:components element))))

(defn- is-component [element]
  (record? element))

(defn move-component
  ([component left top mode]
   (d/set-left component (if (= :offset mode) (+ (d/get-left component) left) left))
   (d/set-top component (if (= :offset mode) (+ (d/get-top component) top) top)))
  ([component left top]
   (move-component component left top :absolute)))

(defn move-container [container nleft ntop]
  (let [{:keys [left top]} (get-bbox container)
        offset-x (- nleft left)
        offset-y (- ntop top)]
     (doseq [component (get-components container)]
       (move-component component offset-x offset-y :offset))))

(defn move-element [element left top]
  (cond
    (is-container element)
    (move-container element left top)
    (is-component element)
    (move-component element left top)))

(defn bbox [element]
  (cond
    (is-container element)
    (get-bbox element)
    (is-component element)
    (d/get-bbox element)))

(defn- resolve-options [container options]
  (if (fn? options)
    (options container)
    options))

(defn- contextualize [container opts]
  (let [options (resolve-options container opts)]
    {:container-bbox (bbox container)
     :options options
     :coords { :top (or (:top options) 0)
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

(defn do-layout [layout container]
  (let [elements ((:select-func layout) container)]
    (reduce (:layout-func layout) (contextualize container (:options layout)) elements)))

(defn do-layouts [container]
  (doseq [layout (-> container :layouts vals)]
    (do-layout layout container)))

(defn having-layout-property [layout-name]
  (fn [container]
    (filterv
      (fn [component]
        (let [layout (-> component :props :layout)]
          (= layout layout-name)))
      (get-components container))))

(defn generic-layout [context element])

(defn align-center [src trg]
  (let [srcCx   (+ (d/get-left src) (/ (d/get-width src) 2))
        srcCy   (+ (d/get-top src) (/ (d/get-height src) 2))
        trgLeft (- srcCx (/ (d/get-width trg) 2))
        trgTop  (- srcCy (/ (d.get-height trg) 2))]
      (d/set-data trg {:left trgLeft :top trgTop})))

(defn intersects? [tbbox obbox]
  (or
   (and (<= (:left tbbox) (:left obbox)) (>= (+ (:left tbbox) (:width tbbox)) (:left obbox))
        (<= (:top tbbox) (:top obbox)) (>= (+ (:top tbbox) (:height tbbox)) (:top obbox)))
   (and (<= (:left tbbox) (:left obbox)) (>= (+ (:left tbbox) (:width tbbox)) (:left obbox))
        (<= (:top obbox) (:top tbbox)) (>= (+ (:top obbox) (:height obbox)) (:top tbbox)))
   (and (<= (:left obbox) (:left tbbox)) (>= (+ (:left obbox) (:width obbox)) (:left tbbox))
        (<= (:top obbox) (:top tbbox)) (>= (+ (:top obbox) (:height obbox)) (:top tbbox)))
   (and (<= (:left obbox) (:left tbbox)) (>= (+ (:left obbox) (:width obbox)) (:left tbbox))
        (<= (:top tbbox) (:top obbox)) (>= (+ (:top tbbox) (:height tbbox)) (:top obbox)))))

(defn initialize [app-state]
  (b/on app-state ["layout.do"] -999 (fn [event]
                                       (when-let [{:keys [container type]} (:context event)]
                                         (do-layout (-> container :layouts type) container)
                                         (b/fire app-state "uncommited.render")
                                         (b/fire app-state "rendering.finish"))))

  (b/on app-state ["layouts.do"] -999 (fn [event]
                                        (when-let [{:keys [container]} (:context event)]
                                          (do-layouts container)
                                          (b/fire app-state "uncommited.render")
                                          (b/fire app-state "rendering.finish")))))
