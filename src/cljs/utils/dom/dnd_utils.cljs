(ns utils.dom.dnd-utils
  (:require [utils.dom.dom-utils :as dom]))

(defn data-transfer [event] (.-dataTransfer (.-originalEvent event)) )
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
  (.-files (data-transfer)))

(defn data-transfer-type [event]
  (let [type (get-dnd-data event "m-key")]
    (if (nil? type) "file" type)))


(defmulti dispatch-drop-event data-transfer-type)

(defmethod dispatch-drop-event "tool-data" [event]
  (println (:name (get-dnd-data event "tool-data")))
  {:tool (get-dnd-data event "tool-data")
   :context (event-layer-coords event)}
)

(defmethod dispatch-drop-event "imgid" [event]
  {:data (dom/by-id (get-dnd-data event "imgid"))
   :params (event-layer-coords event)
   :type "dom"}
)

(defmethod dispatch-drop-event "text/html" [event]
 {:data (get-dnd-data event "text/html")
  :params (event-layer-coords event)
  :type "dom"}
)
