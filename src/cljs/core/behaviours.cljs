(ns core.behaviours
  (:require [core.events :as ev]
            [core.features :as f]
            [core.eventbus :as bus]))

(defrecord Behaviour [name
                      display-name
                      type
                      features
                      event-name-provider
                      handler])

(defn build-event-name
  ([component-types event-name]
    (fn [target]
       (mapv #(ev/event-name (:type target) % event-name) component-types)))
  ([event-name]
    (fn [target]
      [(ev/event-name (:type target) nil event-name)])))

(defn add-behaviour [app-state name display-name type features event-provider handler]
  (swap! app-state assoc-in
    [:behaviours :definitions name]
    (Behaviour. name display-name type features event-provider handler))
  name)

(defn validate [behaviour target]
  (reduce f/_OR_ false (mapv #(% target) (:features behaviour))))

(defn- render-changes [event-name]
  (bus/after-all event-name (fn [ev]
                              (bus/fire "uncommited.render")
                              (bus/fire "rendering.finish"))))

(defn- attach [app-state event-name behaviour]
  (let [attached-behaviours (or (get-in @app-state [:behaviours :attached event-name]) [])
        with-new-behaviour (conj attached-behaviours (:name behaviour))]
    (swap! app-state assoc-in [:behaviours :attached event-name] with-new-behaviour)))

(defn- is-attached [app-state event-name behaviour]
  (let [attached-behaviours (get-in @app-state [:behaviours :attached event-name])
        attached (filter #(== (:name behaviour) %) (or attached-behaviours []))]
    (not (empty? attached))))

(defn- reg [app-state event-names behaviour]
  (doseq [event-name event-names]
    (when-not (is-attached app-state event-name behaviour)
      (bus/on [event-name] (:handler behaviour))
      (attach app-state event-name behaviour)
      (render-changes event-name))))

(defn autowire [app-state target]
  (doseq [behaviour (vals (get-in @app-state [:behaviours :definitions]))]
     (when (validate behaviour target)
        (let [event-name-provider (:event-name-provider behaviour)
              event-names (event-name-provider target)]
            (reg app-state event-names behaviour)))))

(defn trigger-behaviour [entity avalue component event-suffix data]
  (bus/fire (ev/event-name (:type entity) (:type component) event-suffix) data))

(bus/on ["entity.component.added"] -999 (fn [event]
                                            (let [context (:context event)
                                                  entity  (:entity context)
                                                  app-state (:app-state context)]
                                              (autowire app-state entity))))
