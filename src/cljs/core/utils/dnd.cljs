(ns core.utils.dnd
  (:require [core.utils.dom :as dom]))

(defn data-transfer [event] (.-dataTransfer (.-originalEvent event)))
(defn event-layer-coords [event] {:left (.-layerX (.-originalEvent event))
                                  :top  (.-layerY (.-originalEvent event))})

(defn- set-data [event key value]
    (.setData (data-transfer event) key value)
    (.setData (data-transfer event) "m-key" key))

(defn set-dnd-data "Set dataTransfer data under choosen key or available data
                    types. Can also set other properties of dataTransfer as
                    effectAllowed. Function used on event when performing dnd."
  ([event val key] (set-data event key val))
  ([event key value allowed-effect]
   (set! (.-effectAllowed (data-transfer event)) allowed-effect)
   (set-data event key value)))

(defn get-dnd-data "Gets dataTransfer dnd event data under given key or selected
                    data type."
  [event key] (.getData (data-transfer event) key))

(defn get-dnd-files [event]
  (.-files (data-transfer event)))

(defn data-transfer-type [event]
  (let [type (get-dnd-data event "m-key")]
    (if (nil? type) "file" type)))


(defmulti dispatch-drop-event data-transfer-type)
