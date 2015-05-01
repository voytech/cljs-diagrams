(ns core.entities-tests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var done)])
  (:require [cemerick.cljs.test :as t]
            [core.entities.entity :as entity]
            [core.entities.entities-impl :as entities]
            [utils.dom.dom-utils :refer [time-now] ]))
