(ns cljs-diagrams.impl.diagrams.commons.tools
  (:require [cljs-diagrams.core.tools :as t]
            [cljs-diagrams.impl.diagrams.commons.nodes :as commons]))

(t/create-tool "Basic Rectangle"
               ""
               :basic-tools
               "/icons/rectangle.svg"
               (t/ctor commons/basic-rect))

(t/create-tool "Rectangle With Icon"
               ""
               :basic-tools
               "/icons/settings.svg"
               (t/ctor commons/rect-with-icon))

(t/create-tool "Entity Container"
               ""
               :basic-tools
               "/icons/container.svg"
               (t/ctor commons/container))

(t/create-tool "Association"
               ""
               :basic-tools
               "/icons/association.svg"
               (t/ctor commons/association))

(t/create-tool "Link"
               ""
               :basic-tools
               "/icons/link.svg"
               (t/ctor commons/link))

(t/create-tool "Notes"
               ""
               :commons
               "http://icons.iconarchive.com/icons/icons8/ios7/128/Editing-Rectangle-icon.png"
               (t/ctor commons/note))
