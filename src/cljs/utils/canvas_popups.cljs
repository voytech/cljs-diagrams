(ns utils.canvas-popups
  (:require [ui.components.popup :as p]
            [utils.popups :as popups]
            [ui.views.editing :as e]))

(def ^:private POPUPS-HOLDER "popups-holder")

(popups/make-popup "editing"
                   POPUPS-HOLDER
                   (e/editing))
