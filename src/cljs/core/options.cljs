(ns core.options)

(def DEFAULT_SIZE_OPTS {:width 180 :height 150})
(def TRANSPARENT_FILL {:fill "rgb(255,255,255)"})
(def DEFAULT_FILL {:fill "black"})
(def DEFAULT_STROKE {:stroke "black" :strokeWidth 1.5})
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false
                           :lockRotation true
                           :lockScalingX true
                           :lockScalingY true})
(def LOCKED_SCALING       {:lockScalingX true
                           :lockScalingY true})
(def LOCKED_ROTATION      {:lockRotation true})
(def LOCKED_MOVEMENT      {:lockMovementX true
                           :lockMovementY true})
(def NO_DEFAULT_CONTROLS  {:hasControls false
                           :hasBorders false
                           :hasRotatingPoint false})
(def LOCKED (merge LOCKED_MOVEMENT LOCKED_ROTATION LOCKED_SCALING NO_DEFAULT_CONTROLS))
(def INVISIBLE {:visible false})
(def HANDLER_SMALL {:radius 8 :fill "#fff" :stroke "black" :strokeWidth 1.5})
(def HANDLER_SMALLEST {:radius 8 :fill "#fff" :stroke "black" :strokeWidth 1.5})
(def DEFAULT_HIGHLIGHT_OPTIONS {:highlight-color "red"
                                :normal-color "black"
                                :highlight-width 3
                                :normal-width 1.5})
(def CONNECTOR_DEFAULT_OPTIONS (merge DEFAULT_SIZE_OPTS DEFAULT_STROKE RESTRICTED_BEHAVIOUR NO_DEFAULT_CONTROLS))

(def TEXT_DEFAULT_FONT {:fontFamily "calibri"})
(def TEXT_NORMAL_SIZE  {:fontSize 12})
(def TEXT_HEADER_SIZE  {:fontSize 16})
(def TEXT_COLOR        {:color "black"})

(def TEXT_HEADER_DEFAULTS (merge TEXT_HEADER_SIZE TEXT_DEFAULT_FONT TEXT_COLOR {:fontWeight "bold" :textAlign "center"}))
(def TEXT_NORMAL_DEFAULTS (merge TEXT_DEFAULT_FONT TEXT_NORMAL_SIZE TEXT_COLOR))
