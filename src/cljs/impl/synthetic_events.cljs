(ns impl.synthetic-events
  (:require [core.events :as events]
            [core.components :as d]))

(defn mouse-dragging-move-event []
  (events/pattern :mouse-dragging-move
                  [(fn [event process-state]
                     (= (:type event) "mouse-down"))
                   (fn [event process-state]
                     (= (:type event) "mouse-move"))]
                  (fn [event process-state]
                    (events/enrich event (:component event)))))

(defn mouse-move-event []
  (events/pattern :mouse-move
                  [(fn [event process-state]
                     (and (= (:state event) "mouse-dragging-move")
                          (= (:type event) "mouse-up")))]
                  (fn [event process-state]
                    (events/clear-state process-state))))

(defn is-component [component]
  (when (and (not (nil? component)) (d/is-component (:uid component)))
    component))

(defn mouse-in-event []
  (events/pattern :mouse-in
                  [(fn [event process-state]
                     (let [result (and (not= (:state event) "mouse-dragging-move")
                                       (not= (:state event) "mouse-in")
                                       (=    (:type event)  "mouse-move"))]
                         (and
                           (not= (is-component (events/get-context process-state :in-out))
                                 (:component event))
                           (= true result)
                           (not (nil? (:component event))))))]
                  (fn [event process-state]
                    (events/schedule process-state #(events/clear-state process-state) :start)
                    (when-let [previous-component (is-component (events/get-context process-state :in-out))]
                      (events/schedule process-state
                                       (fn []
                                         (events/trigger-bus-event
                                           event (merge (events/enrich event previous-component) {:type "mouse-out"})))
                                       :completed))
                    (events/set-context process-state :in-out (:component event)))))

(defn mouse-out-event []
  (events/pattern :mouse-out
                  [(fn [event process-state]
                     (let [result (and (not= (:state event) "mouse-dragging-move")
                                       (not= (:state event) "mouse-out")
                                       (=    (:type event)  "mouse-move"))
                           previous-component (is-component (events/get-context process-state :in-out))]
                        (and (= true result)
                             (not (nil? previous-component))
                             (not= previous-component (:component event)))))]
                  (fn [event process-state]
                    (events/schedule process-state #(events/clear-state process-state) :start)
                    (let [previous-component (is-component (events/get-context process-state :in-out))]
                      (if-not (nil? (:component event))
                        (do
                          (events/set-context process-state :in-out (:component event))
                          (events/schedule process-state
                            (fn [] (events/trigger-bus-event event {:type "mouse-in"}))
                            :completed))
                        (events/remove-context process-state :in-out))
                      (events/enrich event previous-component)))))

(defn mouse-click-event []
  (events/pattern :mouse-point-click
                  [(fn [event process-state] (= (:type event) "mouse-down"))
                   (fn [event process-state] (= (:type event) "mouse-up"))]
                  (fn [event process-state]
                    (events/schedule process-state #(events/clear-state process-state) :start)
                    {})))
