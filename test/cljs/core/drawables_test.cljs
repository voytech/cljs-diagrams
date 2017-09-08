(ns core.drawables-test
  (:require-macros [cljs.test :refer [deftest testing is async use-fixtures]])
  (:require [cljs.test]
            [core.eventbus :as bus]
            [core.drawables :as drawables]))


(def drawables (atom {}))

(defn drawables-cleanup [f]
  (reset! drawables {})
  (f)
  (reset! drawables {}))

(use-fixtures :each drawables-cleanup)

(deftest test-create-drawable-1 []
  (let [drawable (drawables/create-drawable :circle {})]
    (is (not (nil? drawable)))
    (is (= :circle (:type drawable)))
    (is (not (nil? (:uid drawable))))
    (is (= {} @(:rendering-state drawable)))
    (is (= {:z-index 1} @(:model drawable)))))

(deftest test-create-drawable-with-model []
  (let [drawable (drawables/create-drawable :circle {:radius 20 :left 100 :top 100})]
    (is (not (nil? drawable)))
    (is (= :circle (:type drawable)))
    (is (not (nil? (:uid drawable))))
    (is (nil? (:parent drawable)))
    (is (= {} @(:rendering-state drawable)))
    (is (= 20 (:radius @(:model drawable))))
    (is (= 100 (:left @(:model drawable))))
    (is (= 100 (:top @(:model drawable))))))

(deftest test-property-change-event []
  (let [values (atom [])]
    (bus/on ["drawable.changed"]
      (fn [event]
        (let [context (:context @event)
              drawable (:drawable context)]
           (swap! values concat (:properties context))
           nil)))
    (let [drawable (drawables/create-drawable :circle {:radius 20 :left 100 :top 100})]
      (is (not (nil? drawable)))
      (is (= :circle (:type drawable)))
      (is (not (nil? (:uid drawable))))
      (is (nil? (:parent drawable)))
      (is (= {} @(:rendering-state drawable)))
      (is (= 20 (:radius @(:model drawable))))
      (is (= 100 (:left @(:model drawable))))
      (is (= 100 (:top @(:model drawable)))))
    (is (= [:radius :left :top :z-index] @values))))
