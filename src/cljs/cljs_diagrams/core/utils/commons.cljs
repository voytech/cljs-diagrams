(ns cljs-diagrams.core.utils.commons)

(defn save [key data]
  (let [lstorage (.-localStorage js/window)]
    (.setItem lstorage key data)))

(defn load [key]
  (let [lstorage (.-localStorage js/window)]
    (.getItem lstorage key)))
