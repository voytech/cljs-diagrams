(ns core.entities.entities-impl
  (:require [core.entities.entity :as entities]))


(defn create-image-entity [data params]
  (let [img (js/fabric.Image.
                            data
                            (clj->js params))]
    (entities/create-entity "image" img)))

(defn create-text-entity [text params]
  (let [texte (js/fabric.Text. text (clj->js params))]
    entities/create-entity "text" texte))

(defn create-slot-entity [params]
  (let [slot (js/fabric.Rect. (clj->js params))]
    entities/create-entity "slot" slot))


(defn create-circle-entity [])

(defn create-rect-entity [])
