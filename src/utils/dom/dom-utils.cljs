(ns utils.dom.dom-utils)

(defn by-id [id] (.getElementById js/document id))

(defn console-log [obj] (.log js/window.console obj))

(defn wait-on-element [id callback]
  (if (nil? (by-id id))
    (def interval (.setInterval js/window wait-on-element 50 id callback))
    ((fn []
        (.clearInterval js/window interval)
        (callback id)))
    ))
