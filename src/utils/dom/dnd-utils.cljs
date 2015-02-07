(ns utils.dom.dnd-utils)

(defn data-transfer [event] (.-dataTransfer (.-originalEvent event)) )
(defn set-dnd-data [event val key] (.setData (data-transfer event) key val) )
(defn get-dnd-data [event key] (.getData (data-transfer event) key))
