(ns core.macros
  (:require [cljs.analyzer :as a]))


;; (with-page :page-1 as page (do ))
(defmacro with-page [page-id as page-var & body]
  (cond ;;(not (keyword? page-id))  (throw (Exception. "first form must be keyword"))
        (not (symbol? as))        (throw (Exception. "second form must be symbol"))
        (not (= (name as) "as"))  (throw (Exception. "second form must be 'as' symbol"))
        (not (symbol? page-var))  (throw (Exception. "third form must be symbol")))
  `(let [~page-var (core.project/proj-page-by-id ~page-id)]
     ~@body))

(defmacro with-current-page [as page-var & body]
   (cond
        (not (symbol? as))        (throw (Exception. "second form must be symbol"))
        (not (= (name as) "as"))  (throw (Exception. "second form must be 'as' symbol"))
        (not (symbol? page-var))  (throw (Exception. "third form must be symbol")))
   `(let [~page-var (core.project/proj-selected-page)]
      (when (not (nil? ~page-var)) ~@body)))

(defmacro with-canvas [page-id as canv-var & body]
  `(with-page page-id as page
    (let [~canv-var (:canvas page)] ~@body)))

(defmacro with-current-canvas [as canv-var & body]
  (cond
        (not (symbol? as))        (throw (Exception. "second form must be symbol"))
        (not (= (name as) "as"))  (throw (Exception. "second form must be 'as' symbol"))
        (not (symbol? canv-var))  (throw (Exception. "third form must be symbol")))
  `(let [page-var# (core.project/proj-selected-page)]
     (when (not (nil? page-var#))
       (let [~canv-var (:canvas page-var#)] ~@body))))

(defn transpose-macro [body]
  (apply merge (map (fn [e] {(keyword (name (first e))) (last e)}) body)))

(defmacro defentity [name data options & body]
  (let [transposition (transpose-macro body)]
    (let [drawables (:with-drawables transposition)
          behaviours (:with-behaviours transposition)]
      (when (or (nil? drawables) (nil? behaviours))
        (throw (Error. "Provide drawables and behaviours definition within entitity definition")))
     `(defn ~name [~data ~options]
         (doseq [behaviour# (partition 3 ((fn[] ~behaviours)))]
           (let [entity-name# (name '~name)
                 drawable-name# (first behaviour#)
                 event-type# (second behaviour#)
                 handler# (last behaviour#)]
             (core.entities/handle-event entity-name#
                                         drawable-name#
                                         event-type#
                                         (fn [e#] (when (= (:drawable e#) drawable-name#) (handler# e#))))))
         (let [drawables# (mapv #(core.entities/EntityDrawable. (first %) (second %) (last %)) (partition 3 ((fn[] ~drawables))))]
           (core.entities/create-entity (name '~name) drawables#))))))
