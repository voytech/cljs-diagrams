(ns core.eventbus-test
  (:require-macros [cljs.test :refer [deftest testing is async use-fixtures]])
  (:require [cljs.test]
            [core.eventbus :as bus]))

(defn bus-cleanup [f]
  (reset! bus/bus {})
  (f)
  (reset! bus/bus {}))

(use-fixtures :each bus-cleanup)

(defn- get-registered-handlers [key]
  (get @bus/bus key))

(deftest test-helpers
  (is (= nil (get-registered-handlers "event.testing")))
  (bus/on ["event.testing"] 0 (fn [event] identity))
  (is (= 1 (count (get-registered-handlers "event.testing")))))

(deftest test-register-eventbus-handler []
  (bus/on ["event.testing"] 0 (fn [event] identity))
  (is (= 1 (count (keys @bus/bus))))
  (let [handlers (get-registered-handlers "event.testing")
        handler (first handlers)]
    (is (not (nil? handler)))
    (is (= (:priority handler) 0))))

(deftest test-register-eventbus-handler-2 []
  (bus/on ["event.testing"] (fn [event] identity))
  (is (= 1 (count (keys @bus/bus))))
  (let [handlers (get-registered-handlers "event.testing")
        handler (first handlers)]
    (is (not (nil? handler)))
    (is (= (:priority handler) bus/DEFAULT_PRIORITY))))

(deftest test-register-multiple-handlers []
  (bus/on ["event.testing"] (fn [event] identity))
  (bus/on ["event.testing"] (fn [event] identity))
  (is (= 1 (count (keys @bus/bus))))
  (let [handlers (get-registered-handlers "event.testing")]
    (is (= 2 (count handlers)))))

(deftest test-register-unregister-handler []
  (bus/on ["event.testing"] (fn [event] identity))
  (is (= 1 (count (keys @bus/bus))))
  (bus/off ["event.testing"])
  (is (= 0 (count (keys @bus/bus)))))

(deftest test-once-handler []
  (bus/once "event.testing" (fn [event] :executed))
  (is (= 1 (count (keys @bus/bus))))
  (is (= :executed (bus/fire "event.testing")))
  (is (= 0 (count (keys @bus/bus)))))

(deftest test-fire-an-event []
  (bus/on ["event.testing"] (fn [event] :executed))
  (is (= :executed (bus/fire "event.testing"))))

(deftest test-fire-an-event-verify-default-context []
  (bus/on ["event.testing"] (fn [event]
                              (is (= {} (:context @event)))
                              (is (not (nil? (:timestamp @event))))
                              (is (not (nil? (:uid @event))))
                              (is (= false (:cancelBubble @event)))
                              (is (= false (:defaultPrevented @event)))
                              (is (= "event.testing" (:type @event)))
                              :executed))
  (is (= :executed (bus/fire "event.testing"))))

(deftest test-fire-an-event-verify-context []
  (bus/on ["event.testing"] (fn [event]
                              (is (= {:data "some-data"} (:context @event)))
                              (is (not (nil? (:timestamp @event))))
                              (is (not (nil? (:uid @event))))
                              (is (= false (:cancelBubble @event)))
                              (is (= false (:defaultPrevented @event)))
                              (is (= "event.testing" (:type @event)))
                              :executed))
  (is (= :executed (bus/fire "event.testing" {:data "some-data"}))))

(deftest test-fire-on-multiple-handlers []
  (let [aggregate (atom [])]
    (bus/on ["event.testing"] (fn [event] (swap! aggregate conj (:context @event))
                                          nil))
    (bus/on ["event.testing"] (fn [event] (swap! aggregate conj (:context @event))
                                          nil))
    (bus/on ["event.testing"] (fn [event] (swap! aggregate conj (:context @event))
                                          nil))
    (bus/fire "event.testing" 1)
    (is (= [1 1 1] @aggregate))))

(deftest test-fire-with-explicit-stop-propagation []
  (let [aggregate (atom [])]
    (bus/on ["event.testing"] 0 (fn [event] (swap! aggregate conj (:context @event))
                                            nil))
    (bus/on ["event.testing"] 1 (fn [event] (swap! aggregate conj (:context @event))
                                            (bus/stop-propagation event)))
    (bus/on ["event.testing"] 2 (fn [event] (swap! aggregate conj (:context @event))
                                            nil))
    (bus/fire "event.testing" 1)
    (is (= [1 1] @aggregate))))

(deftest test-fire-with-return-and-stop-propagation []
  (let [aggregate (atom [])]
    (bus/on ["event.testing"] 0 (fn [event] (swap! aggregate conj (:context @event))
                                            nil))
    (bus/on ["event.testing"] 1 (fn [event] (swap! aggregate conj (:context @event))
                                            2))
    (bus/on ["event.testing"] 2 (fn [event] (swap! aggregate conj (:context @event))
                                            nil))
    (is (= 2 (bus/fire "event.testing" 1)))
    (is (= [1 1] @aggregate))))

(deftest test-fire-with-priorities []
  (let [aggregate (atom [])]
    (bus/on ["event.testing"] 0 (fn [event] (swap! aggregate conj 0)
                                            nil))
    (bus/on ["event.testing"] 2 (fn [event] (swap! aggregate conj 1)
                                            nil))
    (bus/on ["event.testing"] 1 (fn [event] (swap! aggregate conj 2)
                                            nil))
    (bus/fire "event.testing")
    (is (= [0 2 1] @aggregate))))
