(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e)))  e}) body)))

(defmacro value [value drawables]
  `(core.entities/AttributeDomain. ~value ~drawables))

(defmacro with-components [data options & components-vector]
  (let [components (if (and (coll? (first components-vector)) (= 1 (count components-vector))) (first components-vector) components-vector)]
    `(fn [~data ~options] (mapv (fn [dd#] (core.entities/Component. (:name dd#)
                                                                    (:type dd#)
                                                                    (:drawable dd#)
                                                                    (:props dd#))) ~components))))
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
        (doseq [component-type# (keys ~behaviours)]
          (let [event-map# (get ~behaviours component-type#)]
            (doseq [event-type# (keys event-map#)]
              (let [handler# (get event-map# event-type#)]
                (core.entities/register-event-handler :entity (name '~name) component-type# event-type# handler#)))))
        (defn ~name [data# options#]
           (let [e# (core.entities/create-entity (name '~name) {} ~cntbbox)
                 component-factory# ~components]
             (apply core.entities/add-entity-component (cons e# (component-factory# data# options#)))
             (doseq [call# ~attributes] (call# e#))
             (core.entities/entity-by-id (:uid e#))))))))

(defmacro defattribute [name & body]
  (let [transformed    (transform-body body)
        dfinition      (last (:with-definition transformed))
        has-definition (contains? transformed :with-definition)
        components      (:with-components  transformed)
        has-components  (contains? transformed :with-drawables)
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
                                               (:sync ~dfinition)
                                               (if ~has-components
                                                 ~components
                                                 nil))]
           (core.entities/add-attribute attr#)
           (when (not (nil? ~behaviours))
             (doseq [component-type# (keys ~behaviours)]
               (let [event-map# (get ~behaviours component-type#)]
                 (doseq [event-type# (keys event-map#)]
                   (let [handler# (get event-map# event-type#)]
                     (core.entities/register-event-handler :attribute (name '~name) component-type# event-type# handler#))))))
           (defn ~name
             ([entity# data#]
              (~name entity# data# nil))
             ([entity# data# options#]
              (let [attribute#   (core.entities/get-attribute (name '~name))
                    attr-value#  (core.entities/create-attribute-value attribute# data# options#)]
                (core.entities/add-entity-attribute-value entity# attr-value#))))))))

(defmacro defdrawable [name options-defaults]
  `(defn ~name [options#]
     (let [drawable# (core.drawables/create-drawable (keyword (name '~name)) (merge options# ~options-defaults))]
        (core.eventbus/fire ["drawable.created"] {:drawable drawable#})
        drawable#)))
