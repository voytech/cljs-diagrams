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
   (set-data (:drawable e) {:border-color (if bln (:highlight-color options)
                                                  (:normal-color options))
                            :border-width (if bln (:highlight-width options)
                                                  (:normal-width options))})))

(defn show [entity component-name show]
 (let [component (e/get-entity-drawable entity component-name)]
   (setp (:drawable component) :visible show)))
