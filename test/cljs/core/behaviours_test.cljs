(ns core.behaviours-test
  (:require-macros [cljs.test :refer [deftest testing is async use-fixtures]])
  (:require [cljs.test]
            [core.eventbus :as bus]
            [core.drawables :as d]
            [core.behaviours :as b]))

(defn behaviours-cleanup [f]
  (vreset! b/behaviours {})
  (f)
  (vreset! b/behaviours {}))

(use-fixtures :each behaviours-cleanup)

(deftest test-create-behaviour []
  (let [validator (fn [components])
        handler (fn [e])]
      (b/add-behaviour "moving" "Default Entity Moving" :moving validator "mousedrag" handler)
      (let [behaviour (get @b/behaviours "moving")]
        (is (not (nil? behaviour)))
        (is (= validator (:validator behaviour)))
        (is (= handler (:handler behaviour)))
        (is (= "mousedrag" (:action behaviour)))
        (is (= :moving (:type behaviour)))
        (is (= "moving" (:name behaviour)))
        (is (= "Default Entity Moving" (:display-name behaviour))))))

(deftest test-generic-validator []
  (let [validator (b/generic-validator [{:tmpl #{:main :endpoint}
                                         :func (fn [requires types] (= requires types))
                                         :result [:main]}
                                        {:tmpl #{:startpoint :endpoint :relation}
                                         :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                         :result [:startpoint :endpoint]}])
        relation-components [{:name "start" :type :startpoint}
                             {:name "end" :type :endpoint}
                             {:name "connector" :type :relation}]
        not-a-relation      [{:name "start" :type :startpoint}
                             {:name "end" :type :endpoint}]
        rectangle  [{:name "body" :type :main}
                    {:name "left-top" :type :endpoint}]
        rectangle2  [{:name "body" :type :main}
                     {:name "left-top" :type :endpoint}
                     {:name "left-bottom" :type :endpoint}]
        not-a-rect [{:name "body" :type :main}]]
      (is (not (nil? validator)))
      (is (= [:startpoint :endpoint] (validator relation-components)))
      (is (not= [:startpoint :endpoint] (validator not-a-relation)))
      (is (nil? (validator not-a-relation)))
      (is (= [:main] (validator rectangle)))
      (is (= [:main] (validator rectangle2)))
      (is (nil? (validator not-a-rect)))))

(deftest test-autowire []
  (let [handler-result (volatile! {})
        validator (b/generic-validator [{:tmpl #{:main :endpoint}
                                         :func (fn [requires types] (= requires types))
                                         :result [:main]}
                                        {:tmpl #{:startpoint :endpoint :relation}
                                         :func (fn [requires types] (= requires (clojure.set/intersection requires types)))
                                         :result [:startpoint :endpoint]}])
        relation-components [{:name "start" :type :startpoint}
                             {:name "end" :type :endpoint}
                             {:name "connector" :type :relation}]
        rectangle  [{:name "body" :type :main}
                    {:name "left-top" :type :endpoint}
                    {:name "left-bottom" :type :endpoint}]
        handler (fn [e] (vreset! handler-result @e))]

      (b/add-behaviour "moving" "Default Entity Moving" :moving validator "mousedrag" handler)
      (is (= false (bus/is-listener "relation.startpoint.mousedrag")))
      (b/autowire "relation" relation-components)
      (is (= true (bus/is-listener "relation.startpoint.mousedrag")))
      (is (= true (bus/is-listener "relation.endpoint.mousedrag")))
      (bus/fire "relation.startpoint.mousedrag" "fired!")
      (is (= "fired!" (:context @handler-result)))))
