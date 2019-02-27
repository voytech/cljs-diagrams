(ns cljs-diagrams.core.options)

(def DEFAULT_SIZE_OPTS {:width 180 :height 150})

(def WIDTH 180)

(def HEIGHT 150)

(def TRANSPARENT_FILL {:background-color "rgb(255,255,255)"})
(def DEFAULT_FILL {:background-color "black"})
(def DEFAULT_STROKE {:border-color "black" :border-width 1.5})
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :background-color "#fff" :border-color "black" :border-width 1.5})
(def HANDLER_SMALLEST {:radius 8 :background-color "#fff" :border-color "black" :border-width 1.5})
(def DEFAULT_HIGHLIGHT_OPTIONS {:hover-color "red"
                                :normal-color "black"
                                :hover-width 3
                                :normal-width 1.5})

(def CONNECTOR_DEFAULT_OPTIONS (merge DEFAULT_SIZE_OPTS DEFAULT_STROKE))

(def TEXT_DEFAULT_FONT {:font-family "calibri"})
(def TEXT_NORMAL_SIZE  {:font-size 12})
(def TEXT_HEADER_SIZE  {:font-size 16})
(def TEXT_COLOR        {:color "black"})

(def TEXT_HEADER_DEFAULTS (merge TEXT_HEADER_SIZE TEXT_DEFAULT_FONT TEXT_COLOR {:font-weight "bold" :text-align "center"}))
(def TEXT_NORMAL_DEFAULTS (merge TEXT_DEFAULT_FONT TEXT_NORMAL_SIZE TEXT_COLOR))
