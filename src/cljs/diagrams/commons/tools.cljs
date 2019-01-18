(ns diagrams.commons.tools
  (:require [core.tools :as t]
            [impl.entities :as eimpl]
            [diagrams.commons.entities :as commons]))

(t/create-tool "Notes"
               ""
               :commons
               "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
               (t/ctor commons/note))
