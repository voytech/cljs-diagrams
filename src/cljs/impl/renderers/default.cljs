(ns impl.renderers.default)

(defmethod do-render [:fabric :rect] [drawable])


(defmethod create-rendering-state [:fabric :rect] [drawable]
  (js/fabric.Rect. (clj->js (get-bbox drawable))))


(defmethod do-render [:fabric :line] [drawable])


(defmethod create-rendering-state [:fabric :line] [drawable]
  (js/fabric.Line. (clj->js [(get-left drawable)
                             (get-top drawable)
                             (get-width drawable)
                             (get-height drawable)])))


(defmethod do-render [:fabric :triangle] [drawable])


(defmethod create-rendering-state [:fabric :triangle] [drawable]
  (js/fabric.Triangle. (clj->js nil)))
