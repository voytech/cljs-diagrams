(ns impl.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.tools :as t]
            [impl.entities :as eimpl]))

(t/create-tool "Rectangle Node"
               :basic-tools
               (t/ctor eimpl/rectangle-node))

(t/create-tool "Relation"
               :basic-tools
               (t/ctor eimpl/relation [0 0 100 0]))
