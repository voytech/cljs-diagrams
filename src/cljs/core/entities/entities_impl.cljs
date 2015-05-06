(ns core.entities.entities-impl
  (:require [core.entities.entity :as entities]
            [utils.dom.dnd-utils :as dnd]
            [ui.components.popup :as p]
            [utils.popups :as pp]
            [utils.canvas-events-tests :as cet]))

(defn- show-at-func
  ([event-wrapper popup-id bttn]
     (let [event (.-e event-wrapper)
           crd   (cet/client-coords event)]
       (when (= (.-which event) bttn)
         (println (pp/get-popup popup-id))
         (p/show-at (pp/get-popup popup-id) (:x crd) (:y crd)))))
  ([src popup-id]
     (let [crd {:x (.-left src) :y (.-top src)}]
         (println (pp/get-popup popup-id))
         (p/show-at (pp/get-popup popup-id) (:x crd) (:y crd)))))

(defn create-image-entity [data params]
  (let [img (js/fabric.Image.
                            data
                            (clj->js params))]
    (entities/create-entity "image" img
                            {"mouseup" #(show-at-func % "editing" 3)})
                            ))

(defn create-text-entity [text params]
  (let [texte (js/fabric.Text. text (clj->js params))]
    (entities/create-entity "text" texte
                            {"mouseup" #(show-at-func % "text-edit" 3)
                             "added"  #(show-at-func texte "text-create")})))

(defn create-slot-entity [params]
  (let [slot (js/fabric.Rect. (clj->js params))]
    entities/create-entity "slot" slot))


(defn create-circle-entity [])

(defn create-rect-entity [])
