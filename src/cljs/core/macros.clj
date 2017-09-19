(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e)))  e}) body)))

(defmacro value [value drawables]
  `(core.entities/AttributeDomain. ~value ~drawables))

(defmacro with-components [data options & components-vector]
  (let [components (if (and (coll? (first components-vector)) (= 1 (count components-vector))) (first components-vector) components-vector)]
    `(fn [~data ~options] ~components)))

(defmacro with-domain [name body])

(defmacro with-behaviours [name body])

(defmacro with-content-bounding-box [name body])

(defmacro with-attributes [body])

(defmacro defentity [name & body]
  (let [transformed (transform-body body)]
    (let [cntbbox    (last (:with-content-bounding-box transformed))
          components (:with-components transformed)
          behaviours (last (:with-behaviours transformed))
          attributes (last (:with-attributes transformed))]
      (when (nil? components)
        (throw (Error. "Provide components and behaviours definition within entitity definition!")))
      (when (nil? cntbbox)
        (throw (Error. "Provide attribute content bounding box parameters!")))
     `(do
        (defn ~name [data# options#]
           (let [e# (core.entities/create-entity (name '~name) {} ~cntbbox)
                 component-factory# ~components]
             (apply core.entities/add-entity-component e# (component-factory# data# options#))
             (doseq [call# ~attributes] (call# e#))
             (let [result# (core.entities/entity-by-id (:uid e#))]
               (core.behaviours/autowire result#)
               (core.eventbus/fire "entity.render" {:entity result#})
               result#)))))))

(defmacro defattribute [name & body]
  (let [transformed    (transform-body body)
        dfinition      (last (:with-definition transformed))
        has-definition (contains? transformed :with-definition)
        components      (:with-components  transformed)
        has-components  (contains? transformed :with-components)
        behaviours     (last (:with-behaviours transformed))
        has-behaviours (contains? transformed :with-behaviours)
        domain         (last (:with-domain transformed))
        has-domain     (contains? transformed :with-domain)]
    `(when-not (core.entities/is-attribute (name '~name))
         (let [attr# (core.entities/Attribute. (name '~name)
                                               (:cardinality ~dfinition)
                                               (:index ~dfinition)
                                               (if ~has-domain
                                                 ~domain
                                                 nil)
                                               (:bbox ~dfinition)
                                               (if ~has-components
                                                 ~components
                                                 nil))]
           (core.entities/add-attribute attr#)
           (defn ~name
             ([entity# data#]
              (~name entity# data# nil))
             ([entity# data# options#]
              (let [attribute#   (core.entities/get-attribute (name '~name))
                    attr-value#  (core.entities/create-attribute-value attribute# data# options#)]
                (core.behaviours/autowire attr-value#)
                (core.entities/add-entity-attribute-value entity# attr-value#))))))))


(defmacro defdrawable [name options-defaults]
  `(defn ~name [options#]
     (core.drawables/create-drawable (keyword (name '~name)) (merge options# ~options-defaults))))

(defmacro defcomponent [type drawable-ref props init-data]
 `(do (core.entities/define-component (-> '~type name keyword) ~drawable-ref ~props ~init-data)
      (defn ~type
        ([name# data# p#]
         (core.entities/new-component (-> '~type name keyword) name# data# p#))
        ([name# data#]
         (core.entities/new-component (-> '~type name keyword) name# data# {}))
        ([name#]
         (core.entities/new-component (-> '~type name keyword) name# {} {})))))

(defmacro defbehaviour [name display-name type validator action handler]
  `(core.behaviours/add-behaviour (name '~name) ~display-name ~type ~validator ~action ~handler))

(defmacro having-all [& test-types]
  `{:tmpl #{~@test-types}
    :func core.behaviours/having-all-components
    :result true})

(defmacro having-strict [& test-types]
  `{:tmpl #{~@test-types}
    :func core.behaviours/having-strict-components
    :result true})

(defmacro bind-to [& targets]
  `{:result (vector ~@targets)})

(defmacro make-event [transform]
  `~transform)

(defmacro -- [& body]
  `(merge ~@body))

(defmacro validate [& body]
  (let [lst (last body)
        f? (= 'fn (first lst))
        defi (if f? (butlast body) body)]
    `(let [def# (vector ~@defi)]
       (if (= true ~f?)
         (core.behaviours/generic-components-validator def# ~lst)
         (core.behaviours/generic-components-validator def#)))))
