(ns core.layouts)

(defn align-center [src trg]
  (let [srcCx   (+ (.-left src) (/ (.-width src) 2))
        srcCy   (+ (.-top src) (/ (.-width src) 2))
        trgLeft (- srcCx (/ (.-width trg) 2))
        trgTop  (- srcCy (/ (.-height trg) 2))]
      (.set trg (clj->js {:left trgLeft :top trgTop}))
      (.setCoords trg)))

(defn position [partial left top coord-mode])

(defn position-entity [entity left top coord-mode])

(defn position-attribute [attribute left top coord-mode])

(defn get-partials [partials-container]
  (if (not (nil? (:drawables partials-container)))
    (if (map? (:drawables partials-container))
      (vals (:drawables partials-container))
      (:drawables partials-container))
    (throw (Error. "Paramter passed to get-partials is supposed to be partials container but IS NOT !"))))

(defn get-bbox [partials-container]
  (let [values (get-partials partials-container)
        sources (mapv :src values)
        leftmost   (apply min-key (cons #(.-left %) sources))
        rightmost  (apply max-key (cons #(+ (.-left %) (.-width %)) sources))
        topmost    (apply min-key (cons #(.-top %) sources))
        bottommost (apply max-key (cons #(+ (.-top %) (.-height %)) sources))]
    {:left (.-left leftmost)
     :top  (.-top topmost)
     :width (- (+ (.-left rightmost) (.-width rightmost)) (.-left leftmost))
     :height (- (+ (.-top bottommost) (.-height  bottommost)) (.-top topmost))}))

(defn- layout-row [bbox layout-buffer partials-aware]
  (let [partials (get-partials partials-aware)
        partials-bbox (get-bbox partials-aware)
        ;pbbox-right-corner-x (+ (:left partials-bbox) (:width partials-bbox))
        ;pbbox-bottom-corner-y (+ (:top partials-bbox) (:height partials-bbox))
        relative-left #(- (.-left (:src %)) (:left partials-bbox))
        relative-top  #(- (.-top (:src %)) (:top partials-bbox))
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
      (.set (:src partial) (clj->js {:left (+ (:left @layout-buffer) (relative-left partial))
                                     :top  (+ (:top @layout-buffer) (relative-top partial))}))
      (.setCoords (:src partial)))
    (swap! layout-buffer assoc-in [:left] (+ (:left @layout-buffer) (:width partials-bbox)))
    (let [replace? (> (:height partials-bbox) (:row-height @layout-buffer))]
      (when replace? (swap! layout-buffer assoc-in [:row-height] (:height partials-bbox))))))

; Position array of objects (being composed of drawable partials) within given bounding box.
; 1. Get each entry (object composed of drawable partials) bounding box - calculate it from drawables.
; 2. Get each drawable for processed entry and for that drawable get relative possition to bounding box.
; 3.
(defn layout [bbox entries]
  (let [layout-buffer (atom {:row-height 0 :left (:left bbox) :top (:top bbox)})]
    (doseq [partials-aware entries]
      (layout-row bbox layout-buffer partials-aware))))
