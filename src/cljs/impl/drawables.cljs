(ns impl.drawables
  (:require [core.drawables :as drawables])
  (:require-macros [core.macros :refer [defdrawable]]))

(defdrawable rect {})

(defdrawable circle {})

(defdrawable ellipse {})

(defdrawable line {})
