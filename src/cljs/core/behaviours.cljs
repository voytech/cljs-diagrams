(ns core.behaviours
  (:require [core.entities :as e]
            [core.events :as ev]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as bus]))

(defonce behaviours (volatile! {}))

(defonce active-behaviours (volatile! {}))

(defrecord Behaviour [name
                      display-name
                      type
                      validator
                      handler])
;Validator needs to return targets (keyword of component types for which behaviour can be registered)

(defn add-behaviour [name display-name type validator handler]
  (vswap! behaviours assoc name (Behaviour. name display-name type validator handler)))

(defn entity-behaviours [entity-type])

(defn component-behaviours [component-type])

(defn validate [behaviour target]
  (let [behaviour_ (cond
                    (string? behaviour) (get @behaviours behaviour)
                    (record? behaviour) (get @behaviours (:name behaviour)))]
    ((:validator behaviour_) target behaviour_)))

(defn enable [entity component-type behaviour])

(defn disable [entity component-type behaviour])

(defn- make-key [behaviour-type event-name]
   #{behaviour-type event-name})

(defn get-active-behaviour [behaviour-type event-name]
  (let [key (make-key behaviour-type event-name)]
    (get @active-behaviours key)))

(defn- is-active [behaviour-type event-name]
  (not (nil? (get-active-behaviour behaviour-type event-name))))

(defn- set-active-behaviour [behaviour-type event-name behaviour-name]
  (let [key (make-key behaviour-type event-name)]
    (vswap! active-behaviours assoc key behaviour-name)))

(defn- inactive-beahviours [keys]
  (filter #(nil? (get @active-behaviours  %)) keys))

(defn- render-changes [events]
  (doseq [e events]
    (bus/after-all e (fn [ev]
                       (bus/fire "uncommited.render")
                       (bus/fire "rendering.finish")))))

(defn- reg [events behaviour]
  (bus/on events (:handler behaviour))
  (render-changes events))

(defn- with-check-if-already-attached [behaviour events]
  (let [_new (filter #(nil? (get @active-behaviours (make-key (:type behaviour) %))) events)]
    (reg _new behaviour)
    (doseq [n _new]
       (set-active-behaviour (:type behaviour) n (:name behaviour)))))

(defn autowire [target]
  (doseq [behaviour (vals @behaviours)]
     (when-let [results (validate behaviour target)]
        (with-check-if-already-attached behaviour (if (not (coll? results)) [results] results)))))

(defn- to-event [action-fn?]
  (if (fn? action-fn?)
    action-fn?
    (fn [target behaviour result]
       (ev/loose-event-name (:type target) nil result action-fn?))))

(defn generic-components-validator [_definitions action-fn?]
  (fn [target behaviour]
    (let [components (vals (:components target))
          types (set (map :type components))
          transform (to-event action-fn?)]
      (let [attach-to (first (filter #(not (nil? %)) (map (fn [e] (when ((:func e) (:tmpl e) types) (:result e))) _definitions)))]
          (when (not (nil? attach-to))
            (if (coll? attach-to)
              (map #(transform target behaviour %) attach-to)
              (transform target behaviour attach-to)))))))

(defn having-strict-components [test-types actual-types] (= test-types actual-types))

(defn having-all-components [test-types actual-types] (= test-types (clojure.set/intersection test-types actual-types)))

(defonce hooks (atom {}))

(defn trigger-behaviour [entity avalue component event-suffix data]
  (bus/fire (ev/loose-event-name (:type entity) (-> avalue :attribute :name) (:type component) event-suffix) data))
