(ns core.settings
  (:require [tailrecursion.javelin :refer [cell ]])
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(def a4k "A4")
(def a3k "A3")
(def a5k "A5")
(def a6k "A6")

(def a3 {:key a3k :width 297 :height 420})
(def a4 {:key a4k :width 210 :height 297})
(def a5 {:key a5k :width 148 :height 210})
(def a6 {:key a6k :width 105 :height 148})

(def settings (cell {:page-format a4
                     :snapping {
                        :enabled true
                        :visible true
                        :attract 15
                        :interval 50}
                     :multi-page? false
                     :pages {
                             :count 4
                             :two-sided false
                             }
                     :zoom 1}))

(defn settings? [one & path] (cell= (get-in settings (conj path one))))

(defn settings! [val & path] (swap! settings assoc-in path val))
