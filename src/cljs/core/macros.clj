(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e)))  e}) body)))

(defmacro value [value drawables]
  `(core.entities/AttributeDomain. ~value ~drawables))

(defmacro with-drawables [data options & drawables-vector]
  (let [drawables (if (and (coll? (first drawables-vector)) (= 1 (count drawables-vector))) (first drawables-vector) drawables-vector)]
    `(fn [~data ~options] (mapv (fn [dd#] (core.entities/Drawable. (:name dd#)
                                                                   (:type dd#)
                                                                   (:src dd#)
                                                                   (:props dd#))) ~drawables))))
(defmacro with-domain [name body])

(defmacro with-behaviours [name body])

(defmacro with-content-bounding-box [name body])

(defmacro with-attributes [body])

(defmacro defentity [name & body]
  (let [transformed (transform-body body)]
    (let [cntbbox    (last (:with-content-bounding-box transformed))
          drawables  (:with-drawables transformed)
          behaviours (last (:with-behaviours transformed))
          attributes (last (:with-attributes transformed))]
      (when (nil? drawables)
        (throw (Error. "Provide drawables and behaviours definition within entitity definition!")))
      (when (nil? cntbbox)
        (throw (Error. "Provide attribute content bounding box parameters!")))
     `(do
        (doseq [drawable-type# (keys ~behaviours)]
          (let [event-map# (get ~behaviours drawable-type#)]
            (doseq [event-type# (keys event-map#)]
              (let [handler# (get event-map# event-type#)]
                (core.entities/register-event-handler :entity (name '~name) drawable-type# event-type# handler#)))))
        (defn ~name [data# options#]
           (let [e# (core.entities/create-entity (name '~name) {} ~cntbbox)
                 drawable-factory# ~drawables]
             (apply core.entities/add-entity-drawable (cons e# (drawable-factory# data# options#)))
             (doseq [call# ~attributes] (call# e#))
             (core.entities/entity-by-id (:uid e#))))))))

(defmacro defattribute [name & body]
  (let [transformed    (transform-body body)
        dfinition      (last (:with-definition transformed))
        has-definition (contains? transformed :with-definition)
        drawables      (:with-drawables  transformed)
        has-drawables  (contains? transformed :with-drawables)
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
                                               (if ~has-drawables
                                                 ~drawables
                                                 nil))]
           (core.entities/add-attribute attr#)
           (when (not (nil? ~behaviours))
             (doseq [drawable-type# (keys ~behaviours)]
               (let [event-map# (get ~behaviours drawable-type#)]
                 (doseq [event-type# (keys event-map#)]
                   (let [handler# (get event-map# event-type#)]
                     (core.entities/register-event-handler :attribute (name '~name) drawable-type# event-type# handler#))))))
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
