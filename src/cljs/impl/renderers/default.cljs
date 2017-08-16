(ns impl.renderers.default)

(defn- set [property]
  (fn [source value]
    (.set source (clj->js {property value}))))

(defn- properties [:left :top :width :height :x1 :x2 :y1 :y2 :border])

(defonce property-mapping {:left (set :left)})

(defn- property-change-render [drawable rendering-context]
  (let [source (:rendering-state drawable)
        prop   (:property rendering-context)
        new    (:new rendering-context)]
      ((prop property-mapping) source new)
      (.setCoords source)))

(defmethod do-render [:fabric :rect] [drawable rendering-context]
  (property-change-render drawable rendering-context))

(defmethod create-rendering-state [:fabric :rect] [drawable]
  (js/fabric.Rect. (clj->js (get-bbox drawable))))


(defmethod do-render [:fabric :line] [drawable rendering-context]
  (property-change-render drawable rendering-context))

(defmethod create-rendering-state [:fabric :line] [drawable]
  (js/fabric.Line. (clj->js [(get-left drawable)
                             (get-top drawable)
                             (get-width drawable)
                             (get-height drawable)])))


(defmethod do-render [:fabric :triangle] [drawable rendering-context]
  (property-change-render drawable rendering-context))


(defmethod create-rendering-state [:fabric :triangle] [drawable]
  (js/fabric.Triangle. (clj->js nil)))
