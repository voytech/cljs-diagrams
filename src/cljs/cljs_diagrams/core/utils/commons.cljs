(ns cljs-diagrams.core.utils.commons)

(defn save-to-storage [key data]
  (let [lstorage (.-localStorage js/window)]
    (.setItem lstorage key data)))

(defn load-from-storage [key]
  (let [lstorage (.-localStorage js/window)]
    (.getItem lstorage key)))
