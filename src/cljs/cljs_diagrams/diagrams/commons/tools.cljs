(ns cljs-diagrams.diagrams.commons.tools
  (:require [cljs-diagrams.core.tools :as t]
            [cljs-diagrams.impl.entities :as eimpl]
            [cljs-diagrams.diagrams.commons.entities :as commons]))

(t/create-tool "Notes"
               ""
               :commons
               "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
               (t/ctor commons/note))
