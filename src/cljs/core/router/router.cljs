(ns core.router.router
  (:require [utils.dom.dom-utils :as dom]))

(def routes (atom {}))
(def container-id (atom -1))

(defn goto-page [route]
  (set! (.-hash js/location) route))

(defn set-container-id [id]
  (reset! container-id id))

(defn defroute [route page]
  (swap! routes assoc-in [route] page))

; TODO IMPORTANT!!!
; Check what happens with all associated with page dom data which is about to be replaced.
; All associated js objects should be garbage collected.
(defn- on-hash-change []
  (when (= -1 @container-id) (throw (js/Error. "No container specified for router.")))
  (let [location (.-hash js/location)
        page (get @routes location)
        container (dom/by-id @container-id)]
    (when (not (nil? page))
      (dom/remove-childs container)
      (dom/append-child container page))))

(defn route []
  (.on js/window "hashchange" #(on-hash-change)))
