(ns cljs-diagrams.core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e)))  e}) body)))

(defn resolve-namespace-name []
  (let [file-struct (-> a/*cljs-file* slurp read-string)]
    (name (second file-struct))))

(defmacro defp [name & body]
  `(do
    (cljs-diagrams.core.funcreg.provider '~name (fn ~@body))
    (defn ~name [& args#]
      (apply cljs-diagrams.core.funcreg.serialize '~name args#))))

(defmacro component [func & body]
  `(fn [app-state# entity#]
     (~func app-state# entity# ~@body)))

(defmacro component-template [type property-map]
  `{~type ~property-map})

(defmacro components-templates [ & body]
  `(merge ~@body))

(defmacro with-layouts [ & layouts]
  `(fn [app-state# entity#]
     (reduce (fn [agg# layout#]
               (cljs-diagrams.core.entities/add-layout app-state# agg# layout#))
             entity#
             (vector ~@layouts))))

(defmacro resolve-data [data]
  `~data)

(defmacro with-components [context & components]
  `(fn [app-state# entity# ~context]
     (let [left# (or (:left ~context) 0)
           top#  (or (:top  ~context) 0)]
       (reduce (fn [agg# func#] (func# app-state# agg#)) entity# (vector ~@components)))))

(defmacro defcomponent-group [group-name & components]
  `(defn ~group-name [app-state# entity#]
     (reduce (fn [agg# func#] (func# app-state# agg#)) entity# (vector ~@components))))

(defmacro defbehaviour [name display-name type features event-name-provider handler]
  (let [nsname (resolve-namespace-name)]
    `(defn ~name [app-state#]
       (cljs-diagrams.core.behaviours/add-behaviour app-state#
                                      (keyword ~nsname (name '~name))
                                      ~display-name
                                      (keyword ~nsname (name '~type))
                                      ~features
                                      ~event-name-provider
                                      ~handler))))

(defmacro with-tags [ & body]
  `(vector ~@body))

(defmacro defentity [name size & body]
  (let [transformed   (transform-body body)]
    (let [nsname      (resolve-namespace-name)
          components  (:with-components transformed)
          layouts     (:with-layouts transformed)
          tags        (:with-tags transformed)
          components-props (:components-templates transformed)
          resolve-data  (:resolve-data transformed)
          has-layouts   (contains? transformed :with-layouts)
          has-data      (contains? transformed :resolve-data)
          has-templates (contains? transformed :components-templates)]
      (when (nil? components)
        (throw (Error. "Provide components definition within entitity definition!")))
     `(do
        (defn ~name [app-state# context#]
           (let [e# (cljs-diagrams.core.entities/create-entity app-state#
                                                 (keyword ~nsname (name '~name))
                                                 (or ~tags [])
                                                 (merge ~size {:left (:left context#)
                                                               :top (:top context#)})
                                                 (if ~has-templates ~components-props {}))
                 component-factory# ~components]
             (component-factory# app-state#
                                 (if ~has-layouts (~layouts app-state# e#) e#)
                                 context#)
             (let [result# (cljs-diagrams.core.entities/entity-by-id app-state# (:uid e#))]
               (cljs-diagrams.core.layouts/do-layouts result#)
               (cljs-diagrams.core.rendering/render-entity app-state# result#)
               (when ~has-data
                 (cljs-diagrams.extensions.data-resolvers/apply-data app-state# result# (merge ~resolve-data context#)))
               result#)))))))

(defmacro defcomponent [type {:keys [rendering-method attributes initializer] :as args}]
  (let [nsname (resolve-namespace-name)]
   `(defn ~type [app-state# entity# args-map#]
      (cljs-diagrams.core.entities/add-entity-component app-state#
                                          entity#
                                          (merge ~args
                                                 {:type (keyword ~nsname (name '~type))}
                                                 args-map#
                                                 {:attributes (merge ~attributes (:attributes args-map#))})))))
