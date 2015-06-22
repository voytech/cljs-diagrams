(ns core.router.router
  (:require [utils.dom.dom-utils :as dom]))

(def ^:private routes (atom {}))

; TODO!! Consider using many container per route.
(def container-id (atom -1))

(defn goto-page [route]
  (set! (.-hash (.-location js/window)) route))

(defn set-container-id [id]
  (reset! container-id id))

(defn def-route [route page]
  (swap! routes assoc-in [route] page))

; TODO IMPORTANT!!!
; Check what happens with all associated with page dom data which is about to be replaced.
; All associated js objects should be garbage collected.
(defn- on-hash-change []
  (when (= -1 @container-id) (throw (js/Error. "No container specified for router.")))
  (let [location (.-hash (.-location js/window))
        page (get @routes location)
        container (dom/by-id @container-id)]
    (println (str "location : " location))
    (println (str "container : " container))
    (println (str "page : " page))
    (when (not (nil? page))
      (dom/remove-childs container)
      (dom/append-child container page))))

(defn route []
  (.addEventListener js/window "hashchange" #(on-hash-change)))
