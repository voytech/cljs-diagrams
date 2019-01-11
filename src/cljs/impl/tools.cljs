(ns impl.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.tools :as t]
            [impl.entities :as eimpl]))

(t/create-tool "Basic Rectangle"
               ""
               :basic-tools
               "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
               (t/ctor eimpl/basic-rect))

(t/create-tool "Rectangle With Icon"
              ""
              :basic-tools
              "https://mdn.mozillademos.org/files/6457/mdn_logo_only_color.png"
              (t/ctor eimpl/rect-with-icon))

(t/create-tool "Entity Container"
              ""
              :basic-tools
              "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
              (t/ctor eimpl/container))

(t/create-tool "Association"
               ""
               :basic-tools
               "https://www.freeiconspng.com/uploads/data-connector-icon-7.png"
               (t/ctor eimpl/association))

(t/create-tool "Link"
              ""
              :basic-tools
              "https://www.freeiconspng.com/uploads/data-connector-icon-7.png"
              (t/ctor eimpl/link))               
