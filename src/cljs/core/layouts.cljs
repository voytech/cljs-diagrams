(ns core.layouts
  (:require [core.components :as d]
            [core.eventbus :as b]
            [core.entities :as e]))

(defrecord Layout [layout-func origin-x origin-y left top width height])

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

(defn move-component [component left top]
  (d/set-left component left)
  (d/set-top component top))

(defn move-container [container left top]
  (doseq [component (get-components container)]
     (move-component component left top)))

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
    
(defn do-layout [container elements])

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
