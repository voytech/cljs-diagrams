(ns cljs-diagrams.core.behaviours
  (:require [cljs-diagrams.core.events :as ev]
            [cljs-diagrams.core.features :as f]
            [cljs-diagrams.core.state :as state]
            [cljs-diagrams.core.rendering :as r]
            [cljs-diagrams.core.eventbus :as bus]))

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
  (state/assoc-behaviours-state app-state
    [:definitions name]
    (Behaviour. name display-name type features event-provider handler))
  name)

(defn validate [behaviour target]
  (reduce f/_OR_ false (mapv #(% target) (:features behaviour))))

(defn- render-changes [app-state event-name]
  (bus/after-all app-state event-name (fn [ev] (r/render-changes app-state))))

(defn- attach [app-state event-name behaviour]
  (let [attached-behaviours (or (state/get-in-behaviours-state app-state [:attached event-name]) [])
        with-new-behaviour (conj attached-behaviours (:name behaviour))]
    (state/assoc-behaviours-state app-state [:attached event-name] with-new-behaviour)))

(defn- is-attached [app-state event-name behaviour]
  (let [attached-behaviours (state/get-in-behaviours-state app-state [:attached event-name])
        attached (filter #(== (:name behaviour) %) (or attached-behaviours []))]
    (not (empty? attached))))

(defn- reg [app-state event-names behaviour]
  (doseq [event-name event-names]
    (when-not (is-attached app-state event-name behaviour)
      (bus/on app-state [event-name] (:handler behaviour))
      (attach app-state event-name behaviour)
      (render-changes app-state event-name))))

(defn autowire [app-state target]
  (doseq [behaviour (vals (state/get-in-behaviours-state app-state [:definitions]))]
     (when (validate behaviour target)
        (let [event-name-provider (:event-name-provider behaviour)
              event-names (event-name-provider target)]
            (reg app-state event-names behaviour)))))

(defn trigger-behaviour [app-state node avalue shape event-suffix data]
  (bus/fire app-state (ev/event-name (:type node) (:type shape) event-suffix) data))

(defn initialize [app-state]
  (bus/on app-state ["node.shape.added"] -999 (fn [event]
                                                      (let [context (:context event)
                                                            node  (:node context)
                                                            app-state (:app-state context)]
                                                        (autowire app-state node)))))
