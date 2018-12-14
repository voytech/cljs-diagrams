(ns impl.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.tools :as t]
            [impl.entities :as eimpl]))

(t/create-tool "Basic Rectangle"
               :basic-tools
               (t/ctor eimpl/basic-rect))

(t/create-tool "Rectangle With Icon"
              :basic-tools
              (t/ctor eimpl/rect-with-icon))

(t/create-tool "Entity Container"
              :basic-tools
              (t/ctor eimpl/container))

(t/create-tool "Association"
               :basic-tools
               (t/ctor eimpl/association [0 0 100 0]))
