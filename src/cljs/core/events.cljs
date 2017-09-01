(ns core.events)

(defonce events ["mouseover" "mouseout" "mousein" "mouseup" "mousedown" "mousedrag" "dbclick" "click"])

(defonce patterns (atom {}))

(defn add-pattern [name functions])

(defn evaluate [event]
  (doseq [key (keys @patterns)]
    (let [pattern (key @patterns)])))
