(ns core.router.router
  (:require [utils.dom.dom-utils :as dom]
            [clojure.string :as s]))

(def ^:private routes (atom {}))
; TODO!! Consider using many container per route.
(def container-id (atom -1))
(def current-route (atom "#/"))

(defn goto-page [route]
  (set! (.-hash (.-location js/window)) route))

(defn set-container-id [id]
  (reset! container-id id))

(defn def-route
  ([route page] (swap! routes assoc-in [route] page))
  ([route container element]
     (swap! routes assoc-in [route container] element)))

(defn- transition-valid [route]
  (let [lst (last (s/split route #"/"))
        previous  (s/replace route (re-pattern (str "/" lst)) "")]
    (println (str "Previous route:" previous))
    (when (not (nil? (.first (dom/j-query-attr "data-route" previous))))
      (println "Found previous route!"))
    (not (nil? (.first (dom/j-query-attr "data-route" previous))))))

(defn- inject-route-target [route container target]
  (dom/remove-childs container)
  (dom/attr target "data-route" route)
  (dom/append-child container target))
; TODO IMPORTANT!!!
; Check what happens with all associated with page dom data which is about to be replaced.
; All associated js objects should be garbage collected.
(defn- on-hash-change []
  (when (= -1 @container-id) (throw (js/Error. "No container specified for router.")))
  (let [location (.-hash (.-location js/window))
        elements (get @routes location)
        g-container (or (dom/by-id @container-id) nil)
        transition-ok (transition-valid location)]
    (println (str "container " g-container))
    (when (and (not (nil? elements)) (= true transition-ok))
      (if (map? elements)
        (doseq [key (keys elements)]
          (let [container (dom/by-id key)
                element (get elements key)]
              (inject-route-target location container element)))
        (when (and (not (coll? elements))
                   (not (nil? g-container)))
              (inject-route-target location g-container elements)))
    (reset! current-route location))))

(defn route []
  (.addEventListener js/window "hashchange" #(on-hash-change)))
