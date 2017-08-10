(ns core.behaviours)


(defn all [ & handlers]
  (fn [e]
    (doseq [handler handlers]
      (handler e))))

(defn event-wrap
  ([f]
   (fn [e]
     (let [entity (:entity e)]
       (f entity))))
  ([f & args]
   (fn [e]
     (let [entity (:entity e)
           drawable-name (:drawabe e)]
       (apply f (cons entity (cons drawable-name (vec args))))))))

(defn highlight [bln options]
 (fn [e]
   (.set (:src e) (clj->js {:stroke (if bln (:highlight-color options)
                                            (:normal-color options))
                            :strokeWidth (if bln (:highlight-width options)
                                                 (:normal-width options))}))))

(defn show [entity drawable-name show]
 (let [drawable (e/get-entity-drawable entity drawable-name)]
   (.set (:src drawable) (clj->js {:visible show}))))
