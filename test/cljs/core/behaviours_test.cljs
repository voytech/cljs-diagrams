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

(defn generic-validator [_definitions]
  (fn [components]
    (let [types (set (map :type components))]
      (first (filter #(not (nil? %)) (map (fn [e] (when ((:func e) (:tmpl e) types) (:result e))) _definitions))))))

(deftest test-create-behaviour []
  (let [validator (generic-validator [{:tmpl #{:main :endpoint}
                                       :func (fn [requires types] (= requires types))
                                       :result [:main]}
                                      {:tmpl #{:startpoint :endpoint}
                                       :func (fn [requires types] (= requires types))
                                       :result [:startpoint :endpoint]}])

        handler (fn [e])]
      (b/add-behaviour "moving" "Default Entity Moving" :moving validator "mousedrag" handler)
      (let [behaviour (get @b/behaviours "moving")]
        (is (not (nil? behaviour))))))
