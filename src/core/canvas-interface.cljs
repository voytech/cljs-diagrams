(ns core.canvas-interface
  (:require [utils.dom.dom-utils :as dom]))

(defn js-conj [jscontainer obj]
  (.add jscontainer obj)
  jscontainer
)

(def canvas-sheet (atom))
(def snap-grid (atom {:visible false
                      :interval 50
                      :snap-attract 5}))

(defn make-snap-grid [{:keys [interval snap-attract visible]}]
  (reset! snap-grid {:interval inteval
                     :snap-attract snap-attract
                     :visible visible})
  (dom/console-log (str "Initializing snap grid...[ " interval "," snap-attract "," visible " ]" ))
  (if (= true visible)
    (loop [x 0 y 0]
      (if (= (.getWidth @canvas-sheet) x)
        (dom/console-log x)
        (let [line1 (js/fabric.Line. #js [(js/fabric.Point. 0 y)
                                          (js/fabric.Point. (.getWidth @canvas-sheet) y)
                                          (js/fabric.Point. 0 y)
                                          (js/fabric.Point. (.getWidth @canvas-sheet) y)])]
          (dom/console-log (js/fabric.Point. 0 y))
          (dom/console-log (js/fabric.Point. (.getWidth @canvas-sheet) y ))
          (dom/console-log line1)
          (swap! canvas-sheet js-conj line1)
          (recur (+ 50 x) (+ 50 y))))))
)

(defn initialize [domid]
  (dom/wait-on-element domid (fn [id]
                                 (dom/console-log (str "Initializing canvas with id [ " id " ]."))
                                 (reset! canvas-sheet (js/fabric.Canvas. id ))
                                 (.setDimensions @canvas-sheet (js-obj "width" 600 "height" 600)
                                                               (js-obj "cssOnly" true))
                                 (make-snap-grid {:interval 50
                                                  :snap-attract 5
                                                  :visible true}))))




(defmulti add :type)

(defmethod add "dom" [data]
 (let [photo-node (js/fabric.Image.
                           (:data data)
                           (js-obj "left"(:left (:params data))
                                   "top" (:top  (:params data))
                                   "angle" 0
                                   "opacity" 1))]
    (swap! canvas-sheet js-conj photo-node))
)

(defmethod add "raw" [data])

(defmethod add "url" [data])
