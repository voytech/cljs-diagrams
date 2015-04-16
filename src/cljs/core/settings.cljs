(ns core.settings
  (:require [tailrecursion.javelin :refer [cell ]]
            [core.events :as events])
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(declare settings?)
(declare settings!)

(def a4k "A4")
(def a3k "A3")
(def a5k "A5")
(def a6k "A6")
(def tstk "T")

(def test {:key test :width 594 :height 670 :ratio (/ 594 670)})
(def a3 {:key a3k :width 297 :height 420 :ratio (/ 297 420)})
(def a4 {:key a4k :width 210 :height 297 :ratio (/ 210 297)})
(def a5 {:key a5k :width 148 :height 210 :ratio (/ 148 210)})
(def a6 {:key a6k :width 105 :height 148 :ratio (/ 105 148)})

(def page-formats {
                   "T"  test,
                   "A3" a3,
                   "A4" a4,
                   "A5" a5,
                   "A6" a6
                   })

(def settings (cell {:page-format tstk
                     :snapping {
                        :enabled true
                        :visible true
                        :attract 15
                        :interval 50}
                     :multi-page false
                     :pages {
                             :count 4
                             :two-sided false
                             }
                     :zoom 1}))

(defn settings? [one & path] (cell= (get-in settings (conj path one))))

(defn settings! [val & path] (swap! settings assoc-in path val))

(def page-width  (cell= (:width  (get page-formats (get-in settings [:page-format])))))
(def page-height (cell= (:height (get page-formats (get-in settings [:page-format])))))

(defmethod events/on :settings-event [event]
 (let [payload (:payload event)
       val  (:value payload)
       path (:path payload)]
   (apply settings! val path)
 ))
