(ns utils.canvas-events-tests
  )

(defn client-coords [event]
  {:x (.-clientX event)
   :y (.-clientY event)})
