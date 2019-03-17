(ns cljs-diagrams.impl.synthetic-events
  (:require [cljs-diagrams.core.events :as events]
            [cljs-diagrams.core.shapes :as d]))

(defn mouse-dragging-move-event []
  (events/pattern :mouse-dragging-move
                  [(fn [event process-state]
                     (= (:type event) "mouse-down"))
                   (fn [event process-state]
                     (= (:type event) "mouse-move"))]
                  (fn [event process-state]
                    (events/enrich event (:shape event)))))

(defn mouse-move-event []
  (events/pattern :mouse-move
                  [(fn [event process-state]
                     (and (= (:state event) "mouse-dragging-move")
                          (= (:type event) "mouse-up")))]
                  (fn [event process-state]
                    (events/clear-state process-state))))

(defn is-shape [app-state shape]
  (when (and (not (nil? shape)) (d/is-shape app-state (:uid shape)))
    shape))

(defn mouse-in-event []
  (events/pattern :mouse-in
                  [(fn [event process-state]
                     (let [result (and (not= (:state event) "mouse-dragging-move")
                                       (not= (:state event) "mouse-in")
                                       (=    (:type event)  "mouse-move"))]
                         (and
                           (not= (is-shape (:app-state event) (events/get-context process-state :in-out))
                                 (:shape event))
                           (= true result)
                           (not (nil? (:shape event))))))]
                  (fn [event process-state]
                    (events/schedule process-state #(events/clear-state process-state) :start)
                    (when-let [previous-shape (is-shape (:app-state event) (events/get-context process-state :in-out))]
                      (events/schedule process-state
                                       (fn []
                                         (events/trigger-bus-event
                                           event (merge (events/enrich event previous-shape) {:type "mouse-out"})))
                                       :completed))
                    (events/set-context process-state :in-out (:shape event)))))

(defn mouse-out-event []
  (events/pattern :mouse-out
                  [(fn [event process-state]
                     (let [result (and (not= (:state event) "mouse-dragging-move")
                                       (not= (:state event) "mouse-out")
                                       (=    (:type event)  "mouse-move"))
                           previous-shape (is-shape (:app-state event) (events/get-context process-state :in-out))]
                        (and (= true result)
                             (not (nil? previous-shape))
                             (not= previous-shape (:shape event)))))]
                  (fn [event process-state]
                    (events/schedule process-state #(events/clear-state process-state) :start)
                    (let [previous-shape (is-shape (:app-state event) (events/get-context process-state :in-out))]
                      (if-not (nil? (:shape event))
                        (do
                          (events/set-context process-state :in-out (:shape event))
                          (events/schedule process-state
                            (fn [] (events/trigger-bus-event event {:type "mouse-in"}))
                            :completed))
                        (events/remove-context process-state :in-out))
                      (events/enrich event previous-shape)))))

(defn mouse-click-event []
  (events/pattern :mouse-point-click
                  [(fn [event process-state] (= (:type event) "mouse-down"))
                   (fn [event process-state] (= (:type event) "mouse-up"))]
                  (fn [event process-state]
                    (events/schedule process-state #(events/clear-state process-state) :start)
                    {})))
