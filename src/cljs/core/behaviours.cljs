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

(defn autowire
  ([entity-type components-inputs]
   (doseq [behaviour (vals @behaviours)]
      (when-let [targets (validate behaviour components-inputs)]
         (let [inputs  (into {} (map (fn [e] {(:type e) e}) components-inputs))
               valid-inputs (vals (select-keys inputs targets))
               kv (map (fn [e] {:k (make-key entity-type nil (:type e) (:type behaviour)) :v e}) valid-inputs)
               inactive-kv (filter #(nil? (get @active-behaviours (:k %))) kv)]
            (bus/on (mapv #(ev/event-name entity-type nil (-> % :v :type) (:action behaviour)) inactive-kv) (:handler behaviour))
            (doseq [k inactive-kv] (set-active-behaviour entity-type nil (-> k :v :type) (:type behaviour) (:name behaviour)))))))
  ([entity]
   (let [components (:components entity)
         type (:type entity)]
      (autowire type components))))

(defn generic-validator [_definitions]
  (fn [components]
    (let [types (set (map :type components))]
      (first (filter #(not (nil? %)) (map (fn [e] (when ((:func e) (:tmpl e) types) (:result e))) _definitions))))))

(defonce hooks (atom {}))

(defn trigger-behaviour [entity component event-suffix data]
  (js/console.log (str (:type entity) "." (:type component) "." event-suffix))
  (bus/fire (str (name (:type entity)) "." (name (:type component)) "." event-suffix) data))
