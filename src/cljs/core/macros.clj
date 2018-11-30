(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e)))  e}) body)))

(defn resolve-namespace-name []
  (let [file-struct (-> a/*cljs-file* slurp read-string)]
    (name (second file-struct))))

(defmacro value [value factory]
  `(core.entities/AttributeDomain. ~value ~factory))

(defmacro with-components [data options & components-vector]
  (let [components (first components-vector)]
    `(fn [entities# entity# ~data ~options]
       (let [left# (or (:left ~options) 0)
             top#  (or (:top  ~options) 0)
             _entity# (reduce (fn [agg# func#] (func# entities# agg#)) entity# ~components)]
          (doseq [component# (-> _entity# :components vals)]
            (doseq [vl# [[:left left#] [:top top#]]]
              (let [new-val# (+ (vl# 1) (core.components/getp component# (vl# 0)))]
                (core.components/setp component# (vl# 0) new-val#))))))))

(defmacro with-domain [name body])

(defmacro with-behaviours [name body])

(defmacro layout [name layout-func select-func options]
  `{~name (core.layouts.Layout. ~layout-func ~select-func ~options)})

(defmacro with-layouts [ & body]
  `(merge ~@body))

(defmacro defentity [name & body]
  (let [transformed   (transform-body body)]
    (let [nsname      (resolve-namespace-name)
          components  (:with-components transformed)
          layouts     (:with-layouts transformed)
          has-layouts (contains? transformed :with-layouts)]
      (when (nil? components)
        (throw (Error. "Provide components definition within entitity definition!")))
     `(do
        (defn ~name [entities# data# options#]
           (let [e# (core.entities/create-entity entities# (keyword ~nsname (name '~name)) ~layouts)
                 component-factory# ~components]
             (component-factory# entities# e# data# options#)
             (let [result# (core.entities/entity-by-id entities# (:uid e#))]
               (core.eventbus/fire "entity.render" {:entity result#
                                                    :entities entities#})
               result#)))))))

(defmacro defcomponent [type rendering-method props initializer]
  (let [nsname (resolve-namespace-name)]
   `(defn ~type [entities# entity# name# data# p#]
      (core.entities/add-entity-component entities#
                                          entity#
                                          (keyword ~nsname (name '~type))
                                          name#
                                          data#
                                          (merge ~props p#)
                                          ~rendering-method
                                          ~initializer))))
