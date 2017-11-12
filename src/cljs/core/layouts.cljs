(ns core.layouts
  (:require [core.components :as d]
            [core.eventbus :as b]
            [core.entities :as e]))

(defrecord Layout [layout-func options])

(defn get-components [container]
  (if (not (nil? (:components container)))
    (if (map? (:components container))
      (vals (:components container))
      (:components container))
    (throw (Error. "Paramter passed to get-components is supposed to be component container but IS NOT !"))))

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

(defn- compute-layout-bbox [layout container]
  (let [container-bbox (get-bbox container)
        {:keys [left top width height origin-x origin-y]} layout]))

(defn- is-container [element]
  (not (nil? (:components container))))

(defn- is-component [element]
  (record? element))

(defn move-component
  ([component left top mode]
   (d/set-left component (if (= :offset mode) (+ (d/get-left component) left) left))
   (d/set-top component (if (= :offset mode) (+ (d/get-top component) top) top)))
  ([component left top]
   (move-component left top :absolute)))

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

(defn- is-absolute-position [parent-container child-element]
  (let [{:keys [left top]} (bbox child-element)
        parent-bbox (bbox parent-container)]
    (and (>= left (:left parent-bbox))
         (<= left (+ (:left parent-bbox) (:width parent-bbox)))
         (>= top (:top parent-bbox))
         (<= top (+ (:top parent-bbox) (:height parent-bbox))))))

(defn- contextualize [container options]
  {:container-bbox (bbox container)
   :options options
   :current-row-top (or (:top options) 0)
   :current-row-height 0
   :current-row-left (or (:left options) 0)})

(defn alter [context append-top append-width]
   (merge context {:current-row-top (+ (:current-row-top context) append-top)
                   :current-row-left (+ (:current-row-left cotnext) append-width)}))

(defn absolute-row-top [context]
  (+ (-> context :container-bbox :top) (:current-row-top context)))

(defn absolute-row-left [context]
  (+ (-> context :container-bbox :left) (:current-row-left context)))

(defn absolute-next-row [context]
  (+ (absolute-row-top context) (:current-row-height context)))

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

(defn exceeds-container-width? [context element]
  (> (+ (absolute-row-left context) (:width (bbox element))) (container-right-edge context)))

(defn exceeds-container-height? [context element]
  (> (+ (absolute-row-top context) (:height (bbox element))) (container-bottom-edge context)))

(defn do-layout [layout container resolve-elements]
  (let [elements (resolve-elements container)
        {:keys [left top width height] } (bbox container)]
    (reduce (:layout-func layout) (contextualize container (:options layout)) elements)))

(defn add [container element])

(defn add-at [container element index])

(defn add-after [container element after])

(defn add-before [container element before])

(defn remove [container element])

(defn align-center [src trg]
  (let [srcCx   (+ (d/get-left src) (/ (d/get-width src) 2))
        srcCy   (+ (d/get-top src) (/ (d/get-height src) 2))
        trgLeft (- srcCx (/ (d/get-width trg) 2))
        trgTop  (- srcCy (/ (d.get-height trg) 2))]
      (d/set-data trg {:left trgLeft :top trgTop})))

(defn- layout-row [bbox layout-buffer partials-aware]
  (when (> (count (get-components partials-aware)) 0)
    (let [partials (get-components partials-aware)
          partials-bbox (get-bbox partials-aware)
          relative-left #(- (d/get-left  %) (:left partials-bbox))
          relative-top  #(- (d/get-top %) (:top partials-bbox))
          absolute-left (:left @layout-buffer)
          absolute-top  (:top  @layout-buffer)
          exceeds-bbox? (>= (+ absolute-left (:width partials-bbox)) (+ (:left bbox) (:width bbox)))
          starts-row?   (= absolute-left (:left bbox))
          new-row?      (and (not starts-row?) exceeds-bbox?)]
      (when new-row?
        (swap! layout-buffer assoc-in [:left] (:left bbox))
        (swap! layout-buffer assoc-in [:top] (+ absolute-top (:row-height @layout-buffer)))
        (swap! layout-buffer assoc-in [:row-height] (:height partials-bbox)))
      (doseq [partial partials]
        (d/set-data partial {:left (+ (:left @layout-buffer) (relative-left partial))
                             :top  (+ (:top @layout-buffer)  (relative-top partial))})
        (b/fire "component.layout.finished" {:component partial}))
      (swap! layout-buffer assoc-in [:left] (+ (:left @layout-buffer) (:width partials-bbox)))
      (let [replace? (> (:height partials-bbox) (:row-height @layout-buffer))]
        (when replace? (swap! layout-buffer assoc-in [:row-height] (:height partials-bbox)))))))

; Position array of objects (being composed of drawable partials) within given bounding box.
; 1. Get each entry (object composed of drawable partials) bounding box - calculate it from drawables.
; 2. Get each drawable for processed entry and for that drawable get relative possition to bounding box.
; 3.
(defn layout [bbox entries]
  (let [layout-buffer (atom {:row-height 0 :left (:left bbox) :top (:top bbox)})]
    (doseq [partials-aware entries]
      (layout-row bbox layout-buffer partials-aware))
    (b/fire "entries.layout" {:entries (:entries entries)})))

(defn layout-attributes [entity]
  (let [bbox (get-bbox entity)
        cbox {:left (+ (:left (e/get-entity-content-bbox entity)) (:left bbox))
              :top  (+ (:top (e/get-entity-content-bbox entity)) (:top bbox))
              :width (:width (e/get-entity-content-bbox entity))
              :height (:height (e/get-entity-content-bbox entity))}]
    (layout cbox (e/get-attributes-values entity))))

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

(b/on ["layout.attributes"] -999 (fn [event]
                                     (layout-attributes (-> event :context))
                                     (b/fire "uncommited.render")
                                     (b/fire "rendering.finish")))
