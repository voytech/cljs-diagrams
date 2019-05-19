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
  `(fn [app-state-n-node#]
     (~func (:new-state app-state-n-node#) (:target app-state-n-node#) ~@body)))

(defmacro shape-template [type property-map]
  `{~type ~property-map})

(defmacro shapes-templates [ & body]
  `(merge ~@body))

(defmacro with-layouts [ & layouts]
  `(fn [app-state-n-node#]
     (reduce #(cljs-diagrams.core.nodes/add-layout (:new-state %1) (:target %1) %2) app-state-n-node# (vector ~@layouts))))

(defmacro resolve-data [data]
  `~data)

(defmacro with-shapes [context & shapes]
  `(fn [app-state-n-node#]
     (reduce #(%2 (:new-state %1) (:target %2)) app-state-n-node# (vector ~@shapes))))

(defmacro defshapes-group [group-name & shapes]
  `(defn ~group-name [app-state-n-node#]
     (reduce #(%2 (:new-state %1) (:target %2)) app-state-n-node# (vector ~@shapes))))

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
        (throw (Error. "Provide shapes definition within node definition!")))
     `(do
        (defn ~name [app-state# context#]
           (let [result# (cljs-diagrams.core.nodes/create-node app-state#
                                                               (keyword ~nsname (name '~name))
                                                               (or ~tags [])
                                                               (merge ~size {:left (:left context#)
                                                                             :top (:top context#)})
                                                               (if ~has-templates ~shapes-props {}))
                 result# (if ~has-layouts (~layouts result#) result#)
                 result# (~shapes result# context#)]
             (if ~has-data
               (cljs-diagrams.extensions.data-resolvers/apply-data (:new-state result#) (:target result#) (merge ~resolve-data context#))
               (-> (:new-state result#)
                   (cljs-diagrams.core.layouts/do-layouts (:target result#))
                   (cljs-diagrams.core.rendering/render-node (:target result#))))))))))


(defmacro defshape [type {:keys [rendering-method attributes initializer] :as args}]
  (let [nsname (resolve-namespace-name)]
   `(defn ~type [app-state# node# args-map#]
      (cljs-diagrams.core.nodes/add-node-shape app-state#
                                               node#
                                               (merge ~args
                                                      {:type (keyword ~nsname (name '~type))}
                                                      args-map#
                                                      {:attributes (merge ~attributes (:attributes args-map#))})))))
