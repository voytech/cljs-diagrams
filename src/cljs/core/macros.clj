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
    `(fn [container# ~data ~options]
       (let [left# (or (:left ~options) 0)
             top#  (or (:top  ~options) 0)
             _container# (reduce (fn [agg# func#] (func# agg#)) container# ~components)]
          (doseq [component# (-> _container# :components vals)]
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
          has-layouts (contains? transformed :with-layouts)
          behaviours  (last (:with-behaviours transformed))
          attributes  (last (:with-attributes transformed))]
      (when (nil? components)
        (throw (Error. "Provide components and behaviours definition within entitity definition!")))
     `(do
        (defn ~name [data# options#]
           (let [e# (core.entities/create-entity (keyword ~nsname (name '~name)) ~layouts)
                 component-factory# ~components]
             (component-factory# e# data# options#)
             (let [result# (core.entities/entity-by-id (:uid e#))]
               (core.eventbus/fire "entity.render" {:entity result#})
               ;(core.eventbus/fire "layout.do" {:container result# :type :attributes})
               result#)))))))

(defmacro defcomponent [type rendering-method props initializer]
  (let [nsname (resolve-namespace-name)]
   `(do (core.components/define-component (keyword ~nsname (name '~type)) ~rendering-method ~props ~initializer)
        (defn ~type [container# name# data# p#]
            (core.entities/add-entity-component container# (keyword ~nsname (name '~type)) name# data# p#)))))
