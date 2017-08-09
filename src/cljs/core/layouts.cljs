(ns core.layouts)

(defn- layout-row [bbox layout-buffer partials-aware]
  (let [partials (vals (:drawables partials-aware))
        left (:left @layout-buffer)
        top  (:top  @layout-buffer)
        most-right  (apply max-key (cons #(+ left (or (-> % :props :relative-left) 0) (.-width (:src %))) partials))
        most-bottom (apply max-key (cons #(+ top  (or (-> % :props :relative-top) 0) (.-height (:src %))) partials))
        exceeds-bbox? (>= (+ (or (-> most-right :props :relative-left) 0) (.-width (:src most-right)) left) (+ (:left bbox) (:width bbox)))
        starts-row?   (= left (:left bbox))
        new-row?      (and (not starts-row?) exceeds-bbox?)]
    (when new-row?
      (swap! layout-buffer assoc-in [:left] (:left bbox))
      (swap! layout-buffer assoc-in [:top] (+ top (:row-height @layout-buffer)))
      (swap! layout-buffer assoc-in [:row-height] (+ (or (-> most-bottom :props :relative-top) 0) (.-height (:src most-bottom)))))
    (doseq [partial partials]
      (.set (:src partial) (clj->js {:left (+ (:left @layout-buffer) (or (-> partial :props :relative-left) 0))
                                     :top  (+ (or (-> partial :props :relative-top) 0) (:top @layout-buffer))}))
      (.setCoords (:src partial)))
    (swap! layout-buffer assoc-in [:left] (+ (:left @layout-buffer) (or (-> most-right :props :relative-left) 0) (.-width (:src most-right))))
    (let [partials-row-height (+ (or (-> most-bottom :props :relative-top) 0) (.-height (:src most-bottom)))
          replace? (> partials-row-height (:row-height @layout-buffer))]
      (when replace?
        (swap! layout-buffer assoc-in [:row-height] partials-row-height)))))

(defn layout [bbox entries]
  (let [layout-buffer (atom {:row-height 0 :left (:left bbox) :top (:top bbox)})]
    (doseq [partials-aware entries]
      (layout-row bbox layout-buffer partials-aware))))
