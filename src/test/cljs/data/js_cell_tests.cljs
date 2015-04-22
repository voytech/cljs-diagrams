(ns data.js-cell-tests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [data.js-cell :refer [is-getter is-setter to-property]]))

(deftest is-getter-test
  (is (= true (is-getter "getFunction")))
  (is (= true (is-getter "isGetter")))
  (is (not (= true (is-getter "setSth")))))
