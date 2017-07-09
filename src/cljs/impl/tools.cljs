(ns impl.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.tools :as t]
            [core.toolctors :as ctors]))


(t/create-tool "Rectangle"
               :basic-tools
               (ctors/create ctors/rect))

(t/create-tool "Connector"
               :basic-tools
               (ctors/create ctors/line))
