(ns impl.drawables
  (:require [core.drawables :as drawables])
  (:require-macros [core.macros :refer [defdrawable]]))

(defdrawable rect {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable circle {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable ellipse {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable line {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable dashed-line  {:z-index 0 :border-color "black" :border-style :dashed :border-width 1})

(defdrawable dotted-line  {:z-index 0 :border-color "black" :border-style :dotted :border-width 1})

(defdrawable trianlge {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable point {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable path {:z-index 0 :border-color "black" :border-style :solid :border-width 1})

(defdrawable text {:z-index 0 :border-color "black" :border-style :solid :border-width 1})
