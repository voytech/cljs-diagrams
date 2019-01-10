(ns diagrams.bpmn.tools
  (:require [core.tools :as t]
            [impl.entities :as eimpl]
            [diagrams.bpmn.entities :as bpmn]))

(t/create-tool "Activity"
               ""
               :bpmn
               "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
               (t/ctor bpmn/activity))

(t/create-tool "Gateway"
              ""
              :bpmn
              "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
              (t/ctor bpmn/gateway))               

(t/create-tool "Start Event"
              ""
              :bpmn
              "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
              (t/ctor bpmn/process-start))

(t/create-tool "End Event"
              ""
              :bpmn
              "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
              (t/ctor bpmn/process-end))
