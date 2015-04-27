(ns core.actions-tests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var done)])
  (:require [cemerick.cljs.test :as t]
            [core.actions :as actions]
            [core.settings :as settings]
            [core.actions-impl :as actions-impl]
            [utils.dom.dom-utils :refer [time-now] ]))

(defn- sample-data []
  (actions/Action. "1"
                 :sample
                 :NEW
                 {:data "data"}
                 (fn [])
                 {:undo "data"}
                 (time-now)))

(def handled (atom '()))

(defn assert-after [milis func]
  (js/setTimeout (fn [] (func) (done)) milis))

(defn- debug-actions
  ([scope]
     (println "------------------------------------------------------")
     (println (str scope " - History."))
     (println "------------------------------------------------------")
     (doseq [action @actions/actions]
       (let [strr  (.stringify js/JSON (clj->js action))]
         (println (str "----" (:uid action) "----"))
         (println strr)
         (println (str "----------------------------------"))))
     (println "------------------------------------------------------"))
  ([] (debug-actions" Actions")))

(defmethod actions/on :sample [action]
  (println "In on action handler")
  (debug-actions)
  (when (not (nil? action)) (swap! handled conj action)))

(deftest create-action-test
   (let [new (sample-data)]
     (is (= :NEW (:status new)))
     (is (= :sample (:action-code new)))
     (is (= "1" (:uid new)))))

(deftest raise-action-test
  (actions/clear-history)
  (let [new (sample-data)]
    (is (= 1 (do (actions/raise new) (count @actions/actions))))
    (is (= new (first @actions/actions)))))

(deftest ^:async handle-action-test
 (actions/clear-history)
 (let [new (sample-data)]
   (actions/run-actions)  ;;activate action handling.
   (is (= 1 (do (actions/raise new) (count @actions/actions))))
    (js/setTimeout
      (fn []
        (is (= 1 (count @handled))   "Number of handled actions should be one.")
        (is (= new (first @handled)) "first handled by dispatch action should be as sample-data ")
        (debug-actions)
        (done))
      2000)
   ))

(deftest ^:async create-settings-action-test []
  (actions/clear-history)
  (actions/run-actions)
  (debug-actions)
  (actions-impl/change-settings! 2 :pages :count)
  (js/setTimeout
      (fn []
        (is (= 1 (count @actions/actions))   "Number of handled actions should be one.")
        (debug-actions)
        (is (= 2  (get-in @settings/settings [:pages :count])))
        (done))
      3000)
)

(deftest ^:async create-mutiple-settings-action-test []
  (actions/clear-history)
  (actions/run-actions)
  (debug-actions)
  (actions-impl/change-settings! true :multi-page)
  (actions-impl/change-settings! 2 :pages :count)
  (actions-impl/change-settings! settings/a6k :page-format)
  (actions-impl/change-settings! 5 :zoom)
  (js/setTimeout (fn []
                       (is (= true (get-in @settings/settings [:multi-page])))
                       (is (= 2 (get-in @settings/settings [:pages :count])))
                       (is (= settings/a6k (get-in @settings/settings [:page-format])))
                       (is (= 5 (get-in @settings/settings [:zoom])))
                       (debug-actions)
                       (done)) 1000))

(deftest ^:async create-multiple-settings-undo-action-test []
  (actions/clear-history)
  (actions/run-actions)
  (debug-actions "Testing Undo - 1) Cleared. ")
  (actions-impl/change-settings! true :multi-page)
  (actions-impl/change-settings! 2 :pages :count)
  (js/setTimeout (fn []
                       (is (= true (get-in @settings/settings [:multi-page])))
                       (is (= 2 (get-in @settings/settings [:pages :count])))
                       (debug-actions "Testing Undo - 2) Before undo. ")
                       (actions/undo)
                       (js/setTimeout (fn []
                                        (is (= 4 (get-in @settings/settings [:pages :count])))
                                        (debug-actions "Testing undo -3) After undo")
                                        (done)) 1000)) 1000)
)
