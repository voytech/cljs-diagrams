(ns impl.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.tools :as t]
            [impl.entities :as eimpl]))

(t/create-tool "Basic Rectangle"
               ""
               :basic-tools
               "/icons/rectangle.svg"
               (t/ctor eimpl/basic-rect))

(t/create-tool "Rectangle With Icon"
              ""
              :basic-tools
              "/icons/settings.svg"
              (t/ctor eimpl/rect-with-icon))

(t/create-tool "Entity Container"
              ""
              :basic-tools
              "/icons/container.svg"
              (t/ctor eimpl/container))

(t/create-tool "Association"
               ""
               :basic-tools
               "/icons/association.svg"
               (t/ctor eimpl/association))

(t/create-tool "Link"
              ""
              :basic-tools
              "/icons/link.svg"
              (t/ctor eimpl/link))
