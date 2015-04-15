(ns core.events-tests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var done)])
  (:require [cemerick.cljs.test :as t]
            [core.events :as events]
            [core.settings :as settings]
            [core.events-impl :as events-impl]
            [utils.dom.dom-utils :refer [time-now] ]))

(defn- sample-data []
  (events/Event. "1"
                 :sample
                 :NEW
                 {:data "data"}
                 (fn [])
                 {:undo "data"}
                 (time-now)))

(def handled (atom '()))

(defn- debug-events []
  (println "------------------------------------------------------")
  (doseq [event @events/events] (println (str (:uid event) (:event-code event) (:status event) (:timestamp event))))
  (println "------------------------------------------------------"))


(defmethod events/on :sample [event]
  (println "In on event handler")
  (debug-events)
  (when (not (nil? event)) (swap! handled conj event)))

(deftest create-event-test
   (let [new (sample-data)]
     (is (= :NEW (:status new)))
     (is (= :sample (:event-code new)))
     (is (= "1" (:uid new)))))

(deftest raise-event-test
  (events/clear-history)
  (let [new (sample-data)]
    (is (= 1 (do (events/raise new) (count @events/events))))
    (is (= new (first @events/events)))))

(deftest ^:async handle-event-test
 (events/clear-history)
 (let [new (sample-data)]
   (events/run-events)  ;;activate event handling.
   (is (= 1 (do (events/raise new) (count @events/events))))
    (js/setTimeout
      (fn []
        (is (= 1 (count @handled))   "Number of handled events shoudl be one.")
        (is (= new (first @handled)) "first handled by dispatch event should be as sample-data event")
        (debug-events)
        (done))
      20001000)
   ))


(deftest ^:async create-settings-event-test []
  (events/clear-history)
  (events/run-events)
  (events-impl/change-settings! 2 :pages :count)
  (js/setTimeout
      (fn []
        (is (= 1 (count @events/events))   "Number of handled events shoudl be one.")
        (debug-events)
        (is (= 2  (get-in @settings/settings [:pages :count])))
        (done))
      3000)
)
