(ns core.tools-tests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var done)])
  (:require [cemerick.cljs.test :as t]
            [core.tools.tool :as tool]
            [core.tools.tools-impl :as tools]
            [utils.dom.dom-utils :refer [time-now] ]))
