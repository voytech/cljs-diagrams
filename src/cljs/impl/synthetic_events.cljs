(ns impl.synthetic-events
  (:require [core.entities :as e]
            [core.events :as events]
            [core.components :as d]))


(defn- mouse-dragging-move-event []
  (events/add-pattern :mouse-dragging-move
                      [(fn [e] (= (:type e) "mouse-down"))
                       (fn [e] (= (:type e) "mouse-move"))]
                      (fn [e] (events/enrich (:component e)))))

(defn- mouse-move-event []
  (events/add-pattern :mouse-move
                      [(fn [e] (and (= (:state e) "mouse-dragging-move") (= (:type e) "mouse-up")))]
                      (fn [e]
                        (events/clear-state))))

(defn- is-component [component]
  (when (and (not (nil? component)) (d/is-component (:uid component)))
    component))

(defn- mouse-in-event []
  (events/add-pattern :mouse-in
                      [(fn [e]
                         (let [result (and (not= (:state e) "mouse-dragging-move")
                                           (not= (:state e) "mouse-in")
                                           (=    (:type e)  "mouse-move"))]
                             (and (not= (is-component (events/get-context :in-out)) (:component e)) (= true result) (not (nil? (:component e))))))]
                      (fn [e]
                        (events/schedule events/clear-state :start)
                        (when-let [previous-component (is-component (events/get-context :in-out))]
                          (events/schedule (fn [] (events/trigger-bus-event e (merge (events/enrich previous-component) {:type "mouse-out"}))) :completed))
                        (events/set-context :in-out (:component e)))))

(defn- mouse-out-event []
  (events/add-pattern :mouse-out
                      [(fn [e]
                         (let [result (and (not= (:state e) "mouse-dragging-move")
                                           (not= (:state e) "mouse-out")
                                           (=    (:type e)  "mouse-move"))
                               previous-component (is-component (events/get-context :in-out))]
                            (and (= true result) (not (nil? previous-component)) (not= previous-component (:component e)))))]
                      (fn [e]
                        (events/schedule events/clear-state :start)
                        (let [previous-component (is-component (events/get-context :in-out))]
                          (if-not (nil? (:component e))
                            (do
                              (events/set-context :in-out (:component e))
                              (events/schedule (fn [] (events/trigger-bus-event e {:type "mouse-in"})) :completed))
                            (events/remove-context :in-out))
                          (events/enrich previous-component)))))

(defn- mouse-click-event []
  (events/add-pattern :mouse-point-click
                      [(fn [e] (= (:type e) "mouse-down"))
                       (fn [e] (= (:type e) "mouse-up"))]
                      (fn [e]
                        (events/schedule events/clear-state :start)
                        {})))

(mouse-dragging-move-event)
(mouse-move-event)
(mouse-in-event)
(mouse-out-event)
(mouse-click-event)

(events/set-original-events-bindings  {"mousedown"  "mouse-down"
                                       "mouseup"    "mouse-up"
                                       "click"      "mouse-click"
                                       "dbclick"    "mouse-db-click"
                                       "mousemove"  "mouse-move"
                                       "mouseenter" "mouse-enter"
                                       "mouseleave" "mouse-leave"})

(events/set-application-events-bindings {"mouse-dragging-move"   "move"
                                         "mouse-in"              "focus"
                                         "mouse-out"             "blur"
                                         "mouse-point-click"     "activate"})
