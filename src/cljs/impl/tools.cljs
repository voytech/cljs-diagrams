(ns impl.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.tools :as t]
            [core.toolctors :as ctors]))


(t/create-tool "Rectangle Node"
               :basic-tools
               (ctors/create ctors/rectangle-node))

(t/create-tool "Relation"
               :basic-tools
               (ctors/create ctors/relation [0 0 100 0]))
