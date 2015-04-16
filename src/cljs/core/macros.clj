(ns core.macros
  (:require [cljs.analyzer :as a]))

;; (with-page :page-1 as page (do ))
(defmacro with-page [page-id as page-var & body]
  (cond ;;(not (keyword? page-id))  (throw (Exception. "first form must be keyword"))
        (not (symbol? as))        (throw (Exception. "second form must be symbol"))
        (not (= (name as) "as"))  (throw (Exception. "second form must be 'as' symbol"))
        (not (symbol? page-var))  (throw (Exception. "third form must be symbol")))
    `(let [~page-var (core.canvas-interface/proj-page-by-id ~page-id)]
       ~@body))

(defmacro with-current-page [as page-var & body])

(defmacro with-canvas-layer [page-id as canv-var & body])

(defmacro with-current-canvas-layer [as canv-var & body])