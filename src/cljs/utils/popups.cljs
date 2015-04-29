(ns utils.popups
  (:require [ui.components.popup :as p]))

(def popups (atom ()))

(defn make-popup [id parent-id popup trigger-registrar]
  (println (str "Adding popup..." id popup))
  (let [popup-rec (p/default-popup id popup)]
    (swap! popups conj popup-rec)
    (p/attach popup-rec parent-id)
    (trigger-registrar popup-rec)))

(defn detach-all []
  (doseq [popup @popups]
    (p/detach popup))
  (reset! popup ()))

(defn hide-all []
  (doseq [popup @popups]
    (p/hide popup)))
