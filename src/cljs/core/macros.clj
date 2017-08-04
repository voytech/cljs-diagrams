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
    (let [drawables (:with-drawables transposition)]
      (when (nil? drawables)
        (throw (Error. "Provide drawables and behaviours definition within entitity definition")))
     `(defn ~name [~data ~options]
         (let [e# (core.entities/create-entity (name '~name) [])]
           (apply core.entities/add-entity-drawable (cons e# ((fn[] ~drawables))))
           (core.entities/entity-by-id (:uid e#)))))))

(defmacro defattribute [name & body]
  `(defonce ~name (core.entities/Attribute. (name `~name)
                                           ~(:cardinality body)
                                           ~(:weight body)
                                           ~(:domain body)
                                           ~(:create body)
                                           ~(:sync body))))
