(ns core.project
 (:require [cljsjs.jquery]
           [cljsjs.fabric]
           [cljsjs.rx]
           [core.utils.dom :as dom]
           [core.utils.dnd :as dnd]
           [core.entities :as e]
           [core.tools :as t]
           [core.eventbus :as b]
           [core.events :as events]
           [core.rendering :as r]
           [core.drawables :as d]
           [impl.renderers.default :as dd]
           [core.layouts :as layouts]))

(defonce project (atom {}))

(defonce project-state (volatile! {}))

(defn get-container []
  (dom/parent (dom/j-query-id (-> @project :id))))

(defn make-selection [targets]
  (vswap! project-state assoc :selection targets))

(defn get-selection []
  (get @project-state :selection))

(defn clear-selection []
  (vswap! project-state dissoc :selection))

(defonce source-events "click dbclick mousemove mousedown mouseup mouseenter mouseleave keypress keydown keyup")

(defn- add-drag-start-pattern []
  (events/add-pattern :mousedrag
                      [(fn [e] (= (:type e) "mousedown"))
                       (fn [e] (= (:type e) "mousemove"))]
                      (fn [e] (events/enrich (:drawable e)))))

(defn- add-drag-end-pattern []
  (events/add-pattern :mousemove
                      [(fn [e] (and (= (:state e) "mousedrag") (= (:type e) "mouseup")))]
                      (fn [e] (events/clear-state))))

(defn- add-mouse-out-pattern []
  (events/add-pattern :mouseout
                      [(fn [e]
                         (let [result (and (not= (:state e) "mousedrag")
                                           (not= (:state e) "mouseout")
                                           (=    (:type e)  "mousemove"))
                               context (events/get-context :mouseout)
                               get-drawable (fn [uid]
                                              (when (and (not (nil? uid)) (d/is-drawable uid))
                                                uid))]
                           (cond
                             (and (= true result) (nil? context)) (do (events/set-context :mouseout {:d (:drawable e) :s true}) false)
                             (and (= true result) (= true (:s context)) (not= (get-drawable (->> context :d :uid)) (->> e :drawable :uid))) true
                             :else false)))]
                      (fn [e]
                        (events/schedule events/clear-state :start)
                        (events/enrich (:d (events/remove-context :mouseout))))))

(defn- add-point-click-pattern []
  (events/add-pattern :mousepointclick
                      [(fn [e] (= (:type e) "mousedown"))
                       (fn [e] (= (:type e) "mouseup"))]
                      (fn [e]
                        (events/schedule events/clear-state :start)
                        {})))

(defn- add-tripple-click-pattern [])

(defn initialize [id {:keys [width height]}]
  (dom/console-log (str "Initializing canvas with id [ " id " ]."))
  (let [data {:canvas (js/fabric.StaticCanvas. id)
              :id id
              :width width
              :height height}]
    (.setWidth (:canvas data) width)
    (.setHeight (:canvas data) height)
    (reset! project data)
    (add-drag-start-pattern)
    (add-drag-end-pattern)
    (add-mouse-out-pattern)
    (add-point-click-pattern)
    (events/dispatch-events id (clojure.string/split source-events #" "))
    (b/fire "rendering.context.update" {:canvas (:canvas data)})))

;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)]
    (t/invoke-tool tool-obj context)))
