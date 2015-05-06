(ns utils.canvas-popups
  (:require [ui.components.popup :as p]
            [utils.popups :as popups]
            [ui.views.editing :as e]
            [ui.views.text-popups :as tp]))

(def ^:private POPUPS-HOLDER "popups-holder")

(popups/make-popup "editing"
                   POPUPS-HOLDER
                   (e/editing))

(popups/make-popup "text-create"
                   POPUPS-HOLDER
                   (tp/text-add-popup))

(popups/make-popup "text-edit"
                   POPUPS-HOLDER
                   (tp/text-edit-popup))
