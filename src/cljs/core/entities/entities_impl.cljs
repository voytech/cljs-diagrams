(ns core.entities.entities-impl
  (:require [core.entities.entity :as entities]
            [utils.dom.dnd-utils :as dnd]
            [utils.dom.dom-utils :as dom]
            [ui.components.popup :as p]
            [utils.popups :as pp]
            [utils.canvas-events-tests :as cet]))

(defn- doc-pos [crd]
  (let [elem (dom/j-query-class "canvas-container")
        parent-pos (.offset elem)]
    {:x (+ (.-left parent-pos) (:x crd))
     :y (+ (.-top  parent-pos) (:y crd))}
    ))

(defn- show-popup-func
  ([event-wrapper popup-id bttn]
     (let [event (.-e event-wrapper)
           crd   (cet/client-coords event)]
       (when (= (.-which event) bttn)
         (println (pp/get-popup popup-id))
         (p/show-at (pp/get-popup popup-id) (:x crd) (:y crd)))))
  ([src popup-id]
     (let [crd (doc-pos {:x (.-left src) :y (.-top src)})]
         (println (pp/get-popup popup-id))
         (p/show-at (pp/get-popup popup-id) (:x crd) (:y crd)))))

(defn create-image-entity [data params]
  (let [img (js/fabric.Image.
                            data
                            (clj->js params))]
    (entities/create-entity "image" img
                            {"mouseup" #(show-popup-func % "editing" 3)})
                            ))

(defn create-text-entity [text params]
   (let [texte (js/fabric.Text. text (clj->js params))]
    (entities/create-entity "text" texte
                            {"mouseup" #(show-popup-func % "text-edit" 3)
                             "added"  #(show-popup-func texte "text-create")})))

(defn create-slot-entity [params]
  (let [aparams (atom params)]
    (if (nil? (:width params)) (swap! aparams assoc :width 100))
    (if (nil? (:height params)) (swap! aparams assoc :height 100))
    (let [slot (js/fabric.Rect. (clj->js @aparams))]
      (entities/create-entity "slot" slot))))


(defn create-circle-entity [])

(defn create-rect-entity [])
