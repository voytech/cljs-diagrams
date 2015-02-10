(ns core.canvas-interface
  (:require [utils.dom.dom-utils :as dom]))

(defn js-conj [jscontainer obj]
  (.add jscontainer obj)
  jscontainer
)

(def canvas-sheet (atom))

(defn initialize [domid]
  (dom/wait-on-element domid (fn [id]
                                 (dom/console-log (str "Initializing canvas with id:" id))
                                 (reset! canvas-sheet (js/fabric.Canvas. id ))
                                 (.setDimensions @canvas-sheet (js-obj "width" 600 "height" 600)
                                                               (js-obj "cssOnly" true)))))



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
