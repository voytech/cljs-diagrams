(ns core.behaviours
  (:require [core.events :as ev]
            [core.features :as f]
            [core.eventbus :as bus]))

(defonce behaviours (volatile! {}))

(defonce attached-behaviours (volatile! {}))

(defrecord Behaviour [name
                      display-name
                      type
                      features
                      event-name-provider
                      handler])

(defn build-event-name
  ([component-types event-name]
    (fn [target]
       (mapv  #(ev/event-name (:type target)
                              (-> target :attribute :name)
                              %
                              event-name) component-types)))
  ([event-name]
    (fn [target]
      [(ev/event-name (:type target) nil nil event-name)])))

(defn add-behaviour [name display-name type features event-provider handler]
  (vswap! behaviours assoc name (Behaviour. name display-name type features event-provider handler)))

(defn validate [behaviour target]
  (let [behaviour_ (cond
                     (string? behaviour) (get @behaviours behaviour)
                     (record? behaviour) (get @behaviours (:name behaviour)))]
    (reduce f/_OR_ false (mapv #(% target) (:features behaviour)))))

(defn enable [entity component-type behaviour])

(defn disable [entity component-type behaviour])

(defn- render-changes [event-name]
  (bus/after-all event-name (fn [ev]
                              (bus/fire "uncommited.render")
                              (bus/fire "rendering.finish"))))

(defn- attach [event-name behaviour]
  (let [attached (or (get @attached-behaviours event-name) [])]
    (vswap! attached-behaviours assoc event-name (conj attached (:name behaviour)))))

(defn- is-attached [event-name behaviour]
  (let [attached (filter #(== (:name behaviour) %) (or (get @attached-behaviours event-name) []))]
    (not (empty? attached))))

(defn- reg [event-names behaviour]
  (doseq [event-name event-names]
    (when-not (is-attached event-name behaviour)
      (bus/on [event-name] (:handler behaviour))
      (attach event-name behaviour)
      (render-changes event-name))))

(defn autowire [target]
  (doseq [behaviour (vals @behaviours)]
     (when (validate behaviour target)
        (let [event-name-provider (:event-name-provider behaviour)
              event-names (event-name-provider target)]
            (reg event-names behaviour)))))

(defonce hooks (atom {}))

(defn trigger-behaviour [entity avalue component event-suffix data]
  (bus/fire (ev/event-name (:type entity) (-> avalue :attribute :name) (:type component) event-suffix) data))

(bus/on ["entity.component.added"] -999 (fn [event]
                                            (let [context (:context event)
                                                  entity  (:entity context)]
                                              (autowire entity))))

(bus/on ["attribute-value.created"] -999 (fn [event]
                                            (let [context (:context event)
                                                  attribute-value (:attribute-value context)]
                                              (autowire attribute-value))))
