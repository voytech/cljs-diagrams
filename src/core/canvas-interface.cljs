(ns core.canvas-interface
  (:require [utils.dom.dom-utils :as dom]
            [tailrecursion.javelin :refer [cell]]
            [core.settings :refer [settings
                                   settings?
                                   settings!
                                   page-formats
                                   page-width
                                   page-height]])
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(declare add-item)
(defn js-conj [jscontainer obj]
  (.add jscontainer obj)
  jscontainer
)

(defn js-rem [jscontainer obj]
  (.remove jscontainer obj)
  jscontainer
)

(def node-keys-group (atom {}))
(def canvas-buffer (atom {}))
(def canvas-sheet (atom ))
(def layers (atom []))
(def root-id (atom ""))


(defn add-item
  ([item key]
     (swap! canvas-sheet js-conj item)
     (swap! canvas-buffer assoc key item))
  ([item key group]
     (add-item item key)
     (swap! node-keys-group assoc-in [group (count (keys (get @node-keys-group group)))] key)))

(defn del-item [key]
  (let [elem (get @canvas-buffer key)]
    (when (not (nil? elem)) (swap! canvas-sheet js-rem elem))))

(defn del-items [key-group]
  (let [key-map (get @node-keys-group key-group)]
    (doseq [key (keys key-map)]
      (del-item (get key-map key))))
   (swap! node-keys-group dissoc group))

(defn redraw-grid []
  (del-items "grid")
  (when (= true (:visible (:snapping @settings)))
    (loop [x 0 y 0]
      (if (<= (.getWidth @canvas-sheet) x)
         x
        (let [line1 (js/fabric.Rect. (js-obj "left" 0
                                             "top" y
                                             "width" (.getWidth @canvas-sheet)
                                             "height" 1
                                             "opacity" 0.1)),
              line2 (js/fabric.Rect. (js-obj "left" x
                                             "top" 0
                                             "width" 1
                                             "height" (.getHeight @canvas-sheet)
                                             "opacity" 0.1))]
          (doseq [line [line1 line2]]
            (set! (.-selectable line) false)
            (let [key (str "grid" (.-left line) "," (.-top line))]
              (add-item line key "grid")))
          (recur (+ (:interval (:snapping @settings)) x) (+ (:interval (:snapping @settings)) y))))))
)


(defn snap! [target pos-prop pos-prop-set direction]
  (let  [div  (quot (pos-prop target) (:interval (:snapping @settings))),
         rest (mod  (pos-prop target) (:interval (:snapping @settings)))]
    (let [neww (* div (:interval (:snapping @settings)))]
      (when (< rest (:attract (:snapping @settings))) (pos-prop-set target neww)))))

(defn do-snap [event]
  (when (= true (:enabled (:snapping @settings)))
    (let [target (.-target event)]
      (snap! target #(.-left %) #(set! (.-left %) %2) 1)
      (snap! target #(.-top  %) #(set! (.-top %)  %2) 1)))
)

(defn initialize [domid]
  (dom/wait-on-element domid (fn [id]
                               (dom/console-log @settings)
                               (dom/console-log dimm)
                               (reset! root-id id)
                               (dom/console-log (str "Initializing canvas with id [ " id " ]."))
                               (reset! canvas-sheet (js/fabric.Canvas. id ))
                               (cell= (.setDimensions @canvas-sheet (js-obj "width"  page-width
                                                                            "height" page-height)
                                                      (js-obj "cssOnly" true)))
                               (redraw-grid)
                               (.on @canvas-sheet (js-obj "object:moving"
                                                            #(do-snap %))))))
(defn layer-ctor [other-layer])

(defn add-layer "Adds a new canvas layer to the layer holder. New layer can be
                 static or dynamic with control layer on top of it.
                 When creating new layer, one should decide if it should take
                 parameters from exisitng layer e.g. root layer or it can be
                 a sibling layer as in two sided books."
  [id {:keys [static]}]
  (.append (.parent (js/jQuery. (str "#" id))) (layer-ctor (dom/by-id @root-id ))))

(defn get-layer [id])

(defn rem-layer [id])

(defmulti add :type)

(defmethod add "dom" [data]
 (let [photo-node (js/fabric.Image.
                           (:data data)
                           (js-obj "left"(:left (:params data))
                                   "top" (:top  (:params data))
                                   "angle"   0
                                   "opacity" 1))]
    (swap! canvas-sheet js-conj photo-node))
)

(defmethod add "raw" [data])

(defmethod add "url" [data])
