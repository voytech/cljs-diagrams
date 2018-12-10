(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e)))  e}) body)))

(defn resolve-namespace-name []
  (let [file-struct (-> a/*cljs-file* slurp read-string)]
    (name (second file-struct))))

(defmacro with-components [data options & components-vector]
  (let [components (first components-vector)]
    `(fn [app-state# entity# ~data ~options]
       (let [left# (or (:left ~options) 0)
             top#  (or (:top  ~options) 0)
             _entity# (reduce (fn [agg# func#] (func# app-state# agg#)) entity# ~components)]
          (doseq [component# (-> _entity# :components vals)]
            (doseq [vl# [[:left left#] [:top top#]]]
              (let [new-val# (+ (vl# 1) (core.components/getp component# (vl# 0)))]
                (core.components/setp component# (vl# 0) new-val#))))))))

(defmacro defbehaviour [name display-name type features event-name-provider handler]
  (let [nsname (resolve-namespace-name)]
    `(defn ~name [app-state#]
       (core.behaviours/add-behaviour app-state#
                                      (keyword ~nsname (name '~name))
                                      ~display-name
                                      (keyword ~nsname (name '~type))
                                      ~features
                                      ~event-name-provider
                                      ~handler))))

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
        (defn ~name [app-state# data# options#]
           (let [e# (core.entities/create-entity app-state# (keyword ~nsname (name '~name)) ~layouts)
                 component-factory# ~components]
             (component-factory# app-state# e# data# options#)
             (let [result# (core.entities/entity-by-id app-state# (:uid e#))]
               (core.eventbus/fire app-state# "entity.render" {:entity result#})
               result#)))))))

(defmacro defcomponent [type rendering-method props initializer]
  (let [nsname (resolve-namespace-name)]
   `(defn ~type [app-state# entity# name# data# p#]
      (core.entities/add-entity-component app-state#
                                          entity#
                                          (keyword ~nsname (name '~type))
                                          name#
                                          data#
                                          (merge ~props p#)
                                          ~rendering-method
                                          ~initializer))))
