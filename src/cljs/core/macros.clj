(ns core.macros
  (:require [cljs.analyzer :as a]))

(defn transpose-macro [body]
  (apply merge (map (fn [e] {(keyword (name (first e))) (last e)}) body)))

(defmacro defentity [name data options & body]
  (let [transposition (transpose-macro body)]
    (let [cntbbox    (:with-content-bounding-box transposition)
          drawables  (:with-drawables transposition)
          behaviours (:with-behaviours transposition)]
      (when (nil? drawables)
        (throw (Error. "Provide drawables and behaviours definition within entitity definition!")))
      (when (nil? cntbbox)
        (throw (Error. "Provide attribute content bounding box parameters!")))
     `(do
        (doseq [drawable-type# (keys ~behaviours)]
          (let [event-map# (get ~behaviours drawable-type#)]
            (doseq [event-type# (keys event-map#)]
              (let [handler# (get event-map# event-type#)]
                (core.entities/handle-event (name '~name) drawable-type# event-type# handler#)))))
        (defn ~name [~data ~options]
           (let [e# (core.entities/create-entity (name '~name) [] ~cntbbox)]
             (apply core.entities/add-entity-drawable (cons e# ((fn[] ~drawables))))
             (core.entities/entity-by-id (:uid e#))))))))

(defmacro defattribute [name data options dfinition drawables]
  `(defn ~name []
     (when-not (core.entities/is-attribute (name '~name))
       (let [attr# (core.entities/Attribute. (name '~name)
                                            ~(:cardinality dfinition)
                                            ~(:index dfinition)
                                            ~(:domain dfinition)
                                            ~(:bbox dfinition)
                                            ~(:sync dfinition))]
         (core.entities/add-attribute attr#)))
     (fn [entity# ~data]
        (let [~options {:left (:left (core.entities/get-entity-content-bbox entity#))
                        :top  (:top (core.entities/get-entity-content-bbox entity#))}
              attribute#   (core.entities/get-attribute (name '~name))
              attr-value#  (core.entities/create-attribute-value attribute#
                                                                 ~(:value data)
                                                                 ~(:img data)
                                                                 ~drawables)]
          (core.entities/add-entity-attribute-value entity# attr-value#)))))
