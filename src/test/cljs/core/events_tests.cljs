(ns core.events-tests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var done)])
  (:require [cemerick.cljs.test :as t]
            [core.events :as events]))

(defn- sample-data []
  (events/Event. "1"
                 :default
                 :NEW
                 {:data "data"}
                 (fn [])
                 {:undo "data"}
                 ))

(deftest create-event-test
  (let [new (sample-data)]
    (is (= :NEW (:status new)))
    (is (= :default (:event-code new)))
    (is (= "1" (:uid new)))))

(deftest raise-event-test
  (let [new (sample-data)]
    (is (= 1 (do (events/raise new) (count @events/events))))))

(deftest ^:async handle-event-test
 (let [new (sample-data)]
   (is (= 1 (do (events/raise new) (count @events/events))))
   (js/setTimeout
    (fn []
      (is (= true true   ))
      (done))
    2000)))
