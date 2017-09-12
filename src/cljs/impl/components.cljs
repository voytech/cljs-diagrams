(ns impl.components
  (:require [impl.drawables :as d]
            [core.entities :as e])
  (:require-macros [core.macros :refer [defcomponent]]))


(defcomponent relation d/relation-line {} {})

(defcomponent arrow d/arrow {} {})

(defcomponent startpoint d/endpoint {:start "connector" :penultimate true} {:moveable true :visible true :opacity 1})

(defcomponent endpoint d/endpoint {:end "connector"} {:moveable true :visible false :opacity 1})

(defcomponent breakpoint d/endpoint {} {:moveable true :visible true :opacity 1})

(defcomponent control d/control {} {:moveable false :visible false :opacity 1})

(defcomponent main d/rect {} {})

;; Attribute components.

(defcomponent value d/text {} {})

(defcomponent description d/text {} {})
