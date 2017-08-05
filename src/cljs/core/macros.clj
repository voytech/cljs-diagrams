(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transpose-macro [body]
  (apply merge (map (fn [e] {(keyword (name (first e))) (last e)}) body)))

(defmacro defentity [name data options & body]
  (let [transposition (transpose-macro body)]
    (let [drawables (:with-drawables transposition)]
      (when (nil? drawables)
        (throw (Error. "Provide drawables and behaviours definition within entitity definition")))
     `(defn ~name [~data ~options]
         (let [e# (core.entities/create-entity (name '~name) [])]
           (apply core.entities/add-entity-drawable (cons e# ((fn[] ~drawables))))
           (core.entities/entity-by-id (:uid e#)))))))

(defmacro defattribute [name data options dfinition drawables]
  `(defn ~name []
     (when-not (core.entities/is-attribute (name '~name))
       (let [attr# (core.entities/Attribute. (name '~name)
                                            ~(:cardinality dfinition)
                                            ~(:index dfinition)
                                            ~(:domain dfinition)
                                            ~(:sync dfinition))]
         (core.entities/add-attribute attr#)))
     (fn [entity# ~data ~options]
        (let [attribute#   (core.entities/get-attribute (name '~name))
              attr-value#  (core.entities/create-attribute-value attribute#
                                                                 ~(:value data)
                                                                 ~(:img data)
                                                                 ~drawables)]
          (core.entities/add-entity-attribute entity# attr-value#)))))
