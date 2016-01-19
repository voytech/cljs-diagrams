(ns core.entities.entities-impl
  (:require [core.entities.entity :as entities]
            [cljsjs.fabric]
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
         (p/show-at (pp/get-popup popup-id) (:x crd) (:y crd)))))
  ([src popup-id]
     (let [crd (doc-pos {:x (.-left src) :y (.-top src)})]
         (p/show-at (pp/get-popup popup-id) (:x crd) (:y crd)))))

(defn create-image-entity [data params]
  (let [img (js/fabric.Image.
                            data
                            (clj->js params))]
    (entities/create-entity "image" img
                            {"mouseup" #(show-popup-func % "editing" 3)})
                            ))

(defn create-background-entity [data params]
  (let [img (js/fabric.Image. data
                              (clj->js {:originX "left"
                                        :originY "top"
                                        :width 100
                                        :height 100
                                        }))]
      (entities/create-entity "background" img {})))

(defn create-text-entity [text params]
   (let [texte (js/fabric.Text. text (clj->js params))]
    (entities/create-entity "text" texte
                            {"mouseup" #(show-popup-func % "text-edit" 3)
                             "added"  #(show-popup-func texte "text-create")})))

(defn create-slot-entity [params]
  (let [aparams (atom params)]
    (if (nil? (:width params)) (swap! aparams assoc :width 150))
    (if (nil? (:height params)) (swap! aparams assoc :height 150))
    (swap! aparams assoc :fill (str "rgba(0,0,0,0.05)"))
    (swap! aparams assoc :stroke (str "rgb(0,0,0)"))
    (swap! aparams assoc :borderColor (str "rgb(0,0,0)"))
    (let [slot (js/fabric.Rect. (clj->js @aparams))]
      (entities/create-entity "slot" slot
                              {"collide" (fn [src trg])
                               "collide-end" (fn [src trg]
                                               (let [trgsrc (:src trg)
                                                     srcsrc (:src src)]
                                                 (.set trgsrc "width"  (.getWidth srcsrc)) ;;this can be done by map
                                                 (.set trgsrc "height" (.getHeight srcsrc))
                                                 (.set trgsrc "left" (.getLeft srcsrc))
                                                 (.set trgsrc "top" (.getTop srcsrc))
                                                 (.set trgsrc "scaleX" 1)
                                                 (.set trgsrc "scaleY" 1)))}
                              ))))


(defn create-circle-entity [])

(defn create-rect-entity [])

;;dispatch methods for creating different kinds of supported entities.

(defmethod entities/create-entity-for-type "background" [type data-obj]
  (entities/create-entity "background" data-obj {}))

(defmethod entities/create-entity-for-type "text" [type data-obj]
  (entities/create-entity "text" data-obj
                          {"mouseup" #(show-popup-func % "text-edit" 3)
                           "added"  #(show-popup-func data-obj "text-create")}))

(defmethod entities/create-entity-for-type "image" [type data-obj]
  (entities/create-entity "image" data-obj
                          {"mouseup" #(show-popup-func % "editing" 3)})
  )

(defmethod entities/create-entity-for-type "slot" [type data-obj]
  (entities/create-entity "slot" data-obj
                          {"collide" (fn [src trg])
                           "collide-end" (fn [src trg]
                                           (let [trgsrc (:src trg)
                                                 srcsrc (:src src)]
                                             (.set trgsrc "width"  (.getWidth srcsrc)) ;;this can be done by map
                                             (.set trgsrc "height" (.getHeight srcsrc))
                                             (.set trgsrc "left" (.getLeft srcsrc))
                                             (.set trgsrc "top" (.getTop srcsrc))
                                             (.set trgsrc "scaleX" 1)
                                             (.set trgsrc "scaleY" 1)))}
                          ))
