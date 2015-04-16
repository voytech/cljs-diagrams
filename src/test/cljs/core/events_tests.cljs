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

(defn assert-after [milis func]
  (js/setTimeout (fn [] (func) (done)) milis))

(defn- debug-events
  ([scope]
     (println "------------------------------------------------------")
     (println (str scope " - History."))
     (println "------------------------------------------------------")
     (doseq [event @events/events]
       (let [strr  (.stringify js/JSON (clj->js event))]
         (println (str "----" (:uid event) "----"))
         (println strr)
         (println (str "----------------------------------"))))
     (println "------------------------------------------------------"))
  ([] (debug-events " Events ")))

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
      2000)
   ))

(deftest ^:async create-settings-event-test []
  (events/clear-history)
  (events/run-events)
  (debug-events)
  (events-impl/change-settings! 2 :pages :count)
  (js/setTimeout
      (fn []
        (is (= 1 (count @events/events))   "Number of handled events shoudl be one.")
        (debug-events)
        (is (= 2  (get-in @settings/settings [:pages :count])))
        (done))
      3000)
)

(deftest ^:async create-mutiple-settings-event-test []
  (events/clear-history)
  (events/run-events)
  (debug-events)
  (events-impl/change-settings! true :multi-page)
  (events-impl/change-settings! 2 :pages :count)
  (events-impl/change-settings! settings/a6k :page-format)
  (events-impl/change-settings! 5 :zoom)
  (js/setTimeout (fn []
                       (is (= true (get-in @settings/settings [:multi-page])))
                       (is (= 2 (get-in @settings/settings [:pages :count])))
                       (is (= settings/a6k (get-in @settings/settings [:page-format])))
                       (is (= 5 (get-in @settings/settings [:zoom])))
                       (debug-events)
                       (done)) 1000))

(deftest ^:async create-multiple-settings-undo-event-test []
  (events/clear-history)
  (events/run-events)
  (debug-events "Testing Undo - 1) Cleared. ")
  (events-impl/change-settings! true :multi-page)
  (events-impl/change-settings! 2 :pages :count)
  (js/setTimeout (fn []
                       (is (= true (get-in @settings/settings [:multi-page])))
                       (is (= 2 (get-in @settings/settings [:pages :count])))
                       (debug-events "Testing Undo - 2) Before undo. ")
                       (events/undo)
                       (js/setTimeout (fn []
                                        (is (= 4 (get-in @settings/settings [:pages :count])))
                                        (debug-events "Testing undo -3) After undo")
                                        (done)) 1000)) 1000)
)
