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

(defmacro shape [func & body]
  `(fn [app-state# node#]
     (~func app-state# node# ~@body)))

(defmacro shape-template [type property-map]
  `{~type ~property-map})

(defmacro shapes-templates [ & body]
  `(merge ~@body))

(defmacro with-layouts [ & layouts]
  `(fn [app-state# node#]
     (reduce (fn [agg# layout#]
               (cljs-diagrams.core.nodes/add-layout app-state# agg# layout#))
             node#
             (vector ~@layouts))))

(defmacro resolve-data [data]
  `~data)

(defmacro with-shapes [context & shapes]
  `(fn [app-state# node# ~context]
     (let [left# (or (:left ~context) 0)
           top#  (or (:top  ~context) 0)]
       (reduce (fn [agg# func#] (func# app-state# agg#)) node# (vector ~@shapes)))))

(defmacro defshapes-group [group-name & shapes]
  `(defn ~group-name [app-state# node#]
     (reduce (fn [agg# func#] (func# app-state# agg#)) node# (vector ~@shapes))))

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

(defmacro defnode [name size & body]
  (let [transformed   (transform-body body)]
    (let [nsname      (resolve-namespace-name)
          shapes      (:with-shapes transformed)
          layouts     (:with-layouts transformed)
          tags        (:with-tags transformed)
          shapes-props (:shapes-templates transformed)
          resolve-data  (:resolve-data transformed)
          has-layouts   (contains? transformed :with-layouts)
          has-data      (contains? transformed :resolve-data)
          has-templates (contains? transformed :shapes-templates)]
      (when (nil? shapes)
        (throw (Error. "Provide components definition within entitity definition!")))
     `(do
        (defn ~name [app-state# context#]
           (let [e# (cljs-diagrams.core.nodes/create-node app-state#
                                                 (keyword ~nsname (name '~name))
                                                 (or ~tags [])
                                                 (merge ~size {:left (:left context#)
                                                               :top (:top context#)})
                                                 (if ~has-templates ~shapes-props {}))
                 shape-factory# ~shapes]
             (shape-factory# app-state#
                             (if ~has-layouts (~layouts app-state# e#) e#)
                             context#)
             (let [result# (cljs-diagrams.core.nodes/node-by-id app-state# (:uid e#))]
               (cljs-diagrams.core.layouts/do-layouts app-state# result#)
               (cljs-diagrams.core.rendering/render-node app-state# result#)
               (when ~has-data
                 (cljs-diagrams.extensions.data-resolvers/apply-data app-state# result# (merge ~resolve-data context#)))
               result#)))))))

(defmacro defshape [type {:keys [rendering-method attributes initializer] :as args}]
  (let [nsname (resolve-namespace-name)]
   `(defn ~type [app-state# node# args-map#]
      (cljs-diagrams.core.nodes/add-node-shape app-state#
                                          node#
                                          (merge ~args
                                                 {:type (keyword ~nsname (name '~type))}
                                                 args-map#
                                                 {:attributes (merge ~attributes (:attributes args-map#))})))))
