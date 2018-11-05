(ns core.features
  (:require [core.events :as ev]
            [core.eventbus :as bus]))

;; features are:
;; - replacing behaviours
;; - not restricted to registering application bus event handlers
;; - 'deffeature' macro will define a function which takes:
;;    - target as an argument - target may be entity, component or event attribute value
;;    - validator - a function to check if target is compatible with feature
;;    - binder - a function that takes target, executes feature binding logic and returns binder context 
