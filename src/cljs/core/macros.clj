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

(defmacro defentity [name data options & body]
  `(defn ~name [d# o#]
      (let [~data d#
            ~options o#
            components# (into (sorted-map) (partition 2 (vec ~@body)))
            drawables# (eval (:drawables components#))
            behaviours# (:behaviours components#)]
        (js.console/log "asdsad")    
        (doseq [behaviour# (partition 3 behaviours#)]
          (let [entity-name# (name ~name)
                part-name# (first behaviour#)
                event-type# (second behaviour#)
                handler# (last behaviour#)])
          (core.entities/handle-event entity-name#
                                      part-name#
                                      event-type#
                                      (fn [e#] (when (= (:part e#) part-name#) (handler# e#)))))
        (let [parts# (map #(core.entities/Part. (first %) (last %)) (partition 2 drawables#))]
          (core.entities/create-entity (name ~name) parts#)))))
