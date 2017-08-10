(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transform-body [body]
  (apply merge (map (fn [e] {(keyword (name (first e))) (last e)}) body)))

(defn transform-domain [d]
  (mapv (fn [e] {:value (second e) :drawables (second (last e))}) d))

(defmacro defentity [name data options & body]
  (let [transformed (transform-body body)]
    (let [cntbbox    (:with-content-bounding-box transformed)
          drawables  (:with-drawables transformed)
          behaviours (:with-behaviours transformed)
          attributes (:with-attributes transformed)]
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
        (defn ~name [~data ~options]
           (let [e# (core.entities/create-entity (name '~name) [] ~cntbbox)]
             (apply core.entities/add-entity-drawable (cons e# ((fn[] ~drawables))))
             (doseq [call# ~attributes] (call# e#))
             (core.entities/entity-by-id (:uid e#))))))))

(defmacro defattribute [name data options & body]
  (let [transformed (transform-body body)
        dfinition   (:with-definition transformed)
        has-definition (contains? transformed :with-definition)
        drawables   (:with-drawables  transformed)
        has-drawables (contains? transformed :with-drawables)
        behaviours  (:with-behaviours transformed)
        has-behaviours (contains? transformed :with-behaviours)
        domain      (transform-domain (:with-domain transformed))
        has-domain (contains? transformed :with-domain)]
    `(do
       (when-not (core.entities/is-attribute (name '~name))
         (let [attr# (core.entities/Attribute. (name '~name)
                                               (:cardinality ~dfinition)
                                               (:index ~dfinition)
                                               (if ~has-domain  (mapv (fn [dv#] (core.entities/AttributeDomain. (:value dv#) (fn [~data] (:drawables dv#)))) ~domain)
                                                                nil)
                                               (:bbox ~dfinition)
                                               (:sync ~dfinition)
                                               (if ~has-drawables (fn [~data] ~drawables)
                                                                  nil))]
           (core.entities/add-attribute attr#)
           (when (not (nil? ~behaviours))
             (doseq [drawable-type# (keys ~behaviours)]
               (let [event-map# (get ~behaviours drawable-type#)]
                 (doseq [event-type# (keys event-map#)]
                   (let [handler# (get event-map# event-type#)]
                     (core.entities/register-event-handler :attribute (name '~name) drawable-type# event-type# handler#))))))
           (defn ~name [entity# ~data]
             (let [~options {:left (:left (core.entities/get-entity-content-bbox entity#))
                             :top  (:top (core.entities/get-entity-content-bbox entity#))}
                   attribute#   (core.entities/get-attribute (name '~name))
                   attr-value#  (core.entities/create-attribute-value attribute# ~data)]
                (core.entities/add-entity-attribute-value entity# attr-value#))))))))
