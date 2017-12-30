(ns core.behaviours
  (:require [core.events :as ev]
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

(defn- is-all-valid [targets]
  (= 0 (count (filter #(= % false) targets))))

(defn generic-components-validator [_definitions action-fn?]
  (fn [target behaviour]
    (let [transform (to-event action-fn?)]
      (let [attach-targets (filter #(not (nil? %)) (map (fn [e] (when ((:func e) (:tmpl e) target) (:result e))) _definitions))]
        (when (is-all-valid attach-targets)
          (flatten (mapv (fn [attach-to]
                           (when (not (nil? attach-to))
                             (if (coll? attach-to)
                               (map #(transform target behaviour %) attach-to)
                               (transform target behaviour attach-to)))) attach-targets)))))))

(defn- components-types [target]
  (->> target
       :components
       (vals)
       (map :type)
       (set)))

(defn having-strict-components [test-types target] (= test-types (components-types target)))

(defn having-all-components [test-types target] (= test-types (clojure.set/intersection test-types (components-types target))))

(defn any-of-types [test-types target] (contains? test-types (or (:type target) (:name target))))

(defn invalid-when [func target] (func target))

(defonce hooks (atom {}))

(defn trigger-behaviour [entity avalue component event-suffix data]
  (bus/fire (ev/loose-event-name (:type entity) (-> avalue :attribute :name) (:type component) event-suffix) data))

(bus/on ["entity.component.added"] -999 (fn [event]
                                            (let [context (:context event)
                                                  entity  (:entity context)]
                                              (autowire entity))))

(bus/on ["attribute-value.created"] -999 (fn [event]
                                            (let [context (:context event)
                                                  attribute-value (:attribute-value context)]
                                              (autowire attribute-value))))
