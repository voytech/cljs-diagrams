(ns utils.popups
  (:require [ui.components.popup :as p]
            [utils.dom.dom-utils :as dom]))

(def popups (atom {}))

(defn make-popup [id parent-id popup]
  (let [popup-rec (p/default-popup id popup)]
    (swap! popups assoc id popup-rec)
    (dom/wait-on-element parent-id
                         (fn [] (p/attach popup-rec parent-id)))))

(defn detach-all []
  (doseq [popup (vals @popups)]
    (p/detach popup))
  (reset! popups {}))

(defn hide-all []
  (doseq [popup (vals @popups)]
    (p/hide popup)))

(defn get-popup [id]
  (get @popups id))
