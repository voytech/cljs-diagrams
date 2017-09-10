(ns core.layouts
  (:require [core.drawables :as d]
            [core.eventbus :as b]))

(defn align-center [src trg]
  (let [srcCx   (+ (d/get-left src) (/ (d/get-width src) 2))
        srcCy   (+ (d/get-top src) (/ (d/get-height src) 2))
        trgLeft (- srcCx (/ (d/get-width trg) 2))
        trgTop  (- srcCy (/ (d.get-height trg) 2))]
      (d/set-data trg {:left trgLeft :top trgTop})))


(defn position [partial left top coord-mode])

(defn position-entity [entity left top coord-mode])

(defn position-attribute [attribute left top coord-mode])

(defn get-components [partials-container]
  (if (not (nil? (:components partials-container)))
    (if (map? (:components partials-container))
      (vals (:components partials-container))
      (:components partials-container))
    (throw (Error. "Paramter passed to get-partials is supposed to be partials container but IS NOT !"))))

(defn get-bbox [partials-container]
  (when (> (count (get-components partials-container)) 0)
    (let [values (get-components partials-container)
          sources (map :drawable values)
          leftmost   (apply min-key (conj sources #(d/get-left %)))
          rightmost  (apply max-key (conj sources #(+ (d/get-left %) (d/get-width %))))
          topmost    (apply min-key (conj sources #(d/get-top %)))
          bottommost (apply max-key (conj sources #(+ (d/get-top %) (d/get-height %))))]
      {:left (d/get-left leftmost)
       :top  (d/get-top topmost)
       :width (- (+ (d/get-left rightmost) (d/get-width rightmost)) (d/get-left leftmost))
       :height (- (+ (d/get-top bottommost) (d/get-height  bottommost)) (d/get-top topmost))})))

(defn- layout-row [bbox layout-buffer partials-aware]
  (when (> (count (get-components partials-aware)) 0)
    (let [partials (get-components partials-aware)
          partials-bbox (get-bbox partials-aware)
          relative-left #(- (d/get-left (:drawable %)) (:left partials-bbox))
          relative-top  #(- (d/get-top (:drawable %)) (:top partials-bbox))
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
        (d/set-data (:drawable partial) {:left (+ (:left @layout-buffer) (relative-left partial))
                                         :top  (+ (:top @layout-buffer) (relative-top partial))})
        (b/fire "drawable.layout.finished" {:drawable (:drawable partial)}))
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
