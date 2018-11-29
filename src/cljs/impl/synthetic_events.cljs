(ns impl.synthetic-events
  (:require [core.entities :as e]
            [core.events :as events]
            [core.components :as d]))


(defn- mouse-dragging-move-event []
  (events/add-pattern :mouse-dragging-move
                      [(fn [e s] (= (:type e) "mouse-down"))
                       (fn [e s] (= (:type e) "mouse-move"))]
                      (fn [e s] (events/enrich (:component e)))))

(defn- mouse-move-event []
  (events/add-pattern :mouse-move
                      [(fn [e s] (and (= (:state e) "mouse-dragging-move") (= (:type e) "mouse-up")))]
                      (fn [e s]
                        (events/clear-state s))))

(defn- is-component [component]
  (when (and (not (nil? component)) (d/is-component (:uid component)))
    component))

(defn- mouse-in-event []
  (events/add-pattern :mouse-in
                      [(fn [e s]
                         (let [result (and (not= (:state e) "mouse-dragging-move")
                                           (not= (:state e) "mouse-in")
                                           (=    (:type e)  "mouse-move"))]
                             (and (not= (is-component (events/get-context s :in-out)) (:component e)) (= true result) (not (nil? (:component e))))))]
                      (fn [e s]
                        (events/schedule #(events/clear-state s) :start)
                        (when-let [previous-component (is-component (events/get-context s :in-out))]
                          (events/schedule (fn [] (events/trigger-bus-event e (merge (events/enrich previous-component) {:type "mouse-out"}))) :completed))
                        (events/set-context s :in-out (:component e)))))

(defn- mouse-out-event []
  (events/add-pattern :mouse-out
                      [(fn [e s]
                         (let [result (and (not= (:state e) "mouse-dragging-move")
                                           (not= (:state e) "mouse-out")
                                           (=    (:type e)  "mouse-move"))
                               previous-component (is-component (events/get-context s :in-out))]
                            (and (= true result) (not (nil? previous-component)) (not= previous-component (:component e)))))]
                      (fn [e s]
                        (events/schedule #(events/clear-state s) :start)
                        (let [previous-component (is-component (events/get-context s :in-out))]
                          (if-not (nil? (:component e))
                            (do
                              (events/set-context s :in-out (:component e))
                              (events/schedule (fn [] (events/trigger-bus-event e {:type "mouse-in"})) :completed))
                            (events/remove-context s :in-out))
                          (events/enrich previous-component)))))

(defn- mouse-click-event []
  (events/add-pattern :mouse-point-click
                      [(fn [e s] (= (:type e) "mouse-down"))
                       (fn [e s] (= (:type e) "mouse-up"))]
                      (fn [e s]
                        (events/schedule #(events/clear-state s) :start)
                        {})))

(mouse-dragging-move-event)
(mouse-move-event)
(mouse-in-event)
(mouse-out-event)
(mouse-click-event)
