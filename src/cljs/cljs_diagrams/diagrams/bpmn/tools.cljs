(ns cljs-diagrams.diagrams.bpmn.tools
  (:require [cljs-diagrams.core.tools :as t]
            [cljs-diagrams.impl.entities :as eimpl]
            [cljs-diagrams.diagrams.bpmn.entities :as bpmn]))

(t/create-tool "Activity"
               ""
               :bpmn
               "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
               (t/ctor bpmn/activity))

(t/create-tool "Gate"
              ""
              :bpmn
              "/icons/bpmn_gate.svg"
              (t/ctor bpmn/gateway))

(t/create-tool "Start Event"
              ""
              :bpmn
              "/icons/bpmn_start.svg"
              (t/ctor bpmn/process-start))

(t/create-tool "End Event"
              ""
              :bpmn
              "/icons/bpmn_end.svg"
              (t/ctor bpmn/process-end))
