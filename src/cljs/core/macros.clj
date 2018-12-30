(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e)))  e}) body)))

(defn resolve-namespace-name []
  (let [file-struct (-> a/*cljs-file* slurp read-string)]
    (name (second file-struct))))

(defmacro component [func & body]
  `(fn [app-state# entity#]
     (~func app-state# entity# ~@body)))

(defmacro component-template [type property-map]
  `{~type ~property-map})

(defmacro components-templates [ & body ]
  `(merge ~@body))

(defmacro with-components [context & components]
  `(fn [app-state# entity# ~context]
     (let [left# (or (:left ~context) 0)
           top#  (or (:top  ~context) 0)
           _entity# (reduce (fn [agg# func#] (func# app-state# agg#)) entity# (vector ~@components))]
        (doseq [component# (-> _entity# :components vals)]
          (doseq [vl# [[:left left#] [:top top#]]]
            (let [new-val# (+ (vl# 1) (core.components/getp component# (vl# 0)))]
              (core.components/setp component# (vl# 0) new-val#)))))))

(defmacro named-group [group-name & components]
  `(defn ~group-name [app-state# entity#]
     (reduce (fn [agg# func#] (func# app-state# agg#)) entity# (vector ~@components))))

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

(defmacro with-layout [ layout ]
  `~layout)

(defmacro with-tags [ & body]
  `(vector ~@body))

(defmacro with-groups [ ])

(defmacro shape [shape-ref]
  `~shape-ref)

(defmacro defentity [name size & body]
  (let [transformed   (transform-body body)]
    (let [nsname      (resolve-namespace-name)
          components  (:with-components transformed)
          tags        (:with-tags transformed)
          shape-ref   (:shape transformed)
          components-props (:components-templates transformed)
          has-templates (contains? transformed :components-templates)
          layout     (:with-layout transformed)
          has-layout (contains? transformed :with-layout)]
      (when (nil? components)
        (throw (Error. "Provide components definition within entitity definition!")))
     `(do
        (defn ~name [app-state# context#]
           (let [e# (core.entities/create-entity app-state#
                                                 (keyword ~nsname (name '~name))
                                                 (or ~tags [])
                                                 ~layout
                                                 (merge ~size {:left ~context :top ~context})
                                                 (if ~has-templates ~components-props {})
                                                 (or ~shape-ref []))
                 component-factory# ~components]
             (component-factory# app-state# e# context#)
             (let [result# (core.entities/entity-by-id app-state# (:uid e#))]
               (core.eventbus/fire app-state# "entity.render" {:entity result#})
               result#)))))))

(defmacro defcomponent [type rendering-method props initializer layout-hints]
  (let [nsname (resolve-namespace-name)]
   `(defn ~type [app-state# entity# name# data# p# layout-hints#]
      (core.entities/add-entity-component app-state#
                                          entity#
                                          (keyword ~nsname (name '~type))
                                          name#
                                          data#
                                          (merge ~props p#)
                                          ~rendering-method
                                          ~initializer
                                          (or layout-hints# ~layout-hints)))))
