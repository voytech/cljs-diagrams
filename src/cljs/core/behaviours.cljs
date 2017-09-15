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
                      action
                      handler])
;Validator needs to return targets (keyword of component types for which behaviour can be registered)

(defn add-behaviour [name display-name type validator action handler]
  (vswap! behaviours assoc name (Behaviour. name display-name type validator action handler)))

(defn entity-behaviours [entity-type])

(defn component-behaviours [component-type])

(defn validate [behaviour components]
  (let [behaviour_ (cond
                    (string? behaviour) (get @behaviours behaviour)
                    (record? behaviour) (get @behaviours (:name behaviour)))]
    ((:validator behaviour_) components)))

(defn enable [entity component-type behaviour])

(defn disable [entity component-type behaviour])

(defn- make-key [entity-type-or-id attribute-name component-type behaviour-type]
  (cond-> #{}
          (not (nil? entity-type-or-id)) (conj entity-type-or-id)
          (not (nil? attribute-name)) (conj attribute-name)
          (not (nil? component-type)) (conj component-type)
          (not (nil? behaviour-type)) (conj behaviour-type)))

(defn get-active-behaviour [entity-type-or-id attribute-name component-type behaviour-type]
  (let [key (make-key entity-type-or-id attribute-name component-type behaviour-type)]
    (get @active-behaviours key)))

(defn- is-active [entity-type-or-id attribute-name component-type behaviour-type]
  (not (nil? (get-active-behaviour entity-type-or-id attribute-name component-type behaviour-type))))

(defn- set-active-behaviour [entity-type-or-id attribute-name component-type behaviour-type behaviour-name]
  (let [key (make-key entity-type-or-id attribute-name component-type behaviour-type)]
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

(defn- attach-to-entity [entity-type attribute-name behaviour]
  (when (not (is-active entity-type attribute-name nil (:type behaviour)))
    (let [event-name (ev/loose-event-name entity-type attribute-name nil (:action behaviour))]
      (reg [event-name] behaviour)
      (set-active-behaviour entity-type attribute-name nil (:type behaviour) (:name behaviour)))))

(defn- attach-to-components [entity-type attribute-name components-inputs results behaviour]
  (let [inputs (into {} (map (fn [e] {(:type e) e}) components-inputs))
        valid-inputs (vals (select-keys inputs results))
        kv (map (fn [e] {:k (make-key entity-type attribute-name (:type e) (:type behaviour)) :v e}) valid-inputs)
        inactive-kv (filter #(nil? (get @active-behaviours (:k %))) kv)
        events (mapv #(ev/loose-event-name entity-type attribute-name (-> % :v :type) (:action behaviour)) inactive-kv)]
    (js/console.log (clj->js events))
    (reg events behaviour)
    (doseq [k inactive-kv]
      (set-active-behaviour entity-type attribute-name (-> k :v :type) (:type behaviour) (:name behaviour)))))

(defn autowire [entity-type attribute-name components-inputs]
   (doseq [behaviour (vals @behaviours)]
      (when-let [results (validate behaviour components-inputs)]
         (cond
           (= true results) (attach-to-entity entity-type attribute-name behaviour)
           (coll? results)  (attach-to-components entity-type attribute-name components-inputs results behaviour)))))

(defn generic-validator [_definitions]
  (fn [components]
    (let [types (set (map :type components))]
      (first (filter #(not (nil? %)) (map (fn [e] (when ((:func e) (:tmpl e) types) (:result e))) _definitions))))))

(defn having-all [])

(defn having-strict [])

(defonce hooks (atom {}))

(defn trigger-behaviour [entity avalue component event-suffix data]
  (bus/fire (ev/loose-event-name (:type entity) (-> avalue :attribute :name) (:type component) event-suffix) data))
