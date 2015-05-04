(ns core.entities.entities-impl
  (:require [core.entities.entity :as entities]
            [utils.dom.dnd-utils :as dnd]
            [ui.components.popup :as p]
            [utils.popups :as pp]
            [utils.canvas-events-tests :as cet]))


(defn create-image-entity [data params]
  (let [img (js/fabric.Image.
                            data
                            (clj->js params))]
    (entities/create-entity "image" img
                            (fn [src])
                            (fn [event]
                              (let [crd (cet/client-coords event)]
                                (p/show-at (pp/get-popup "editing") (:x crd) (:y crd))))
                            (fn [event])
                            (fn [src trg]))))

(defn create-text-entity [text params]
  (let [texte (js/fabric.Text. text (clj->js params))]
    (entities/create-entity "text" texte)))

(defn create-slot-entity [params]
  (let [slot (js/fabric.Rect. (clj->js params))]
    entities/create-entity "slot" slot))


(defn create-circle-entity [])

(defn create-rect-entity [])
