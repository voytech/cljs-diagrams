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
    (is (nil? (:parent drawable)))
    (is (= {} @(:rendering-state drawable)))
    (is (= {} @(:model drawable)))))

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
  (let [values (atom {})]
    (bus/on ["drawable.changed"]
      (fn [event]
        (let [context (:context @event)]
           (swap! values assoc (:property context) (:new context))
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
    (is (= {:radius 20 :left 100 :top 100} @values))))

(deftest test-drawable-api []
  (let [drawable (drawables/create-drawable :circle {})]
    (is (not (nil? drawable)))
    (drawables/setp drawable :left 20)
    (is (= 20 (:left @(:model drawable))))
    (drawables/set-data drawable {:top 24 :width 100 :height 100})
    (drawables/setp drawable :left 10)
    (is (= {:left 10 :top 24 :width 100 :height 100} @(:model drawable)))
    (drawables/set-border-color drawable "red")
    (drawables/set-background-color drawable "white")
    (drawables/set-border-style drawable :dotted)
    (drawables/set-border-width drawable 1)
    (drawables/set-left drawable 120)
    (drawables/set-top drawable 130)
    (drawables/set-width drawable 200)
    (drawables/set-height drawable 210)
    (is (= {:left 120 :top 130 :width 200 :height 210 :border-width 1 :border-style :dotted :background-color "white" :border-color "red"} @(:model drawable)))
    (is (= 120 (drawables/get-left drawable)))
    (is (= 130 (drawables/get-top drawable)))
    (is (= 200 (drawables/get-width drawable)))
    (is (= 210 (drawables/get-height drawable)))
    (is (= {:left 120 :top 130 :width 200 :height 210} (drawables/get-bbox drawable)))))
