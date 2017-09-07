(ns core.project
 (:require [reagent.core :as reagent :refer [atom]]
           [cljsjs.jquery]
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

(defonce lookup-cache (atom nil))

(defn- lookup [x y]
  (if (and (not (nil? @lookup-cache)) (d/contains-point? @lookup-cache x y))
      @lookup-cache
      (do
        (reset! lookup-cache nil)
        (let [drawable (first (filter (fn [e] (d/contains-point? e x y)) (vals @d/drawables)))]
         (reset! lookup-cache drawable)))))

(defn- lookup-all [x y]
  (->> @d/drawables
       vals
       (filter #(d/contains-point? % x y))
       (sort-by #(d/getp % :z-index) >)))

(defonce event-map {"object:moving" "mousedrag"
                    "mousedown" "mousedown"
                    "mouseup" "mouseup"
                    "click" "mouseclick"
                    "dbclick" "mousedbclick"
                    "mousemove" "mousemove"
                    "mouseenter" "mouseenter"
                    "mouseleave" "mouseleave"})

(defonce source-events "click dbclick mousemove mousedown mouseup mouseenter mouseleave keypress keydown keyup")

(defn- normalise-event-type [event]
  (get event-map event))

(defn- enrich [drawable]
  (when (d/is-drawable (:uid drawable))
    (let [entity             (e/lookup drawable :entity)
          component          (e/lookup drawable :component)
          attribute-value    (e/lookup drawable :attribute)
          drawable           (:drawable component)]
        {:entity           entity
         :attribute-value  attribute-value
         :drawable         drawable
         :component        component})))

(defn- event-name [decomposed]
  (if (nil? (:entity decomposed))
    (:type decomposed)
    (let [entity-type     (as-> decomposed $ (:entity $) (:type $) (str $ "."))
          attribute-type  (if (not (nil? (:attribute-value decomposed)))
                            (as-> decomposed $ (:attribute-value $) (:attribute $) (:name $) (str $ "."))
                            "")
          component-type  (as-> decomposed $ (:component $) (:type $) (name $) (str $ "."))]
       (str entity-type attribute-type component-type (:type decomposed)))))

(defn normalise-event [e obj]
  (let [rect (.getBoundingClientRect obj)
        left (- (.-clientX e) (.-left rect))
        top (- (.-clientY e) (.-top rect))]
     {:source e
      :ctrl-key (.-ctrlKey e)
      :target (.-target e)
      :type (normalise-event-type (.-type e))
      :state (or (:state @events/state) (normalise-event-type (.-type e)))
      :left left
      :top  top
      :movement-x 0
      :movement-y 0}))

(defn- merge-streams [obj events]
  (apply js/Rx.Observable.merge (mapv (fn [e] (js/Rx.Observable.fromEvent obj e)) events)))

(defn- delta-stream [input func]
  (.scan input (fn [acc,e] (merge acc e (func acc e))) {}))

(defn- enriching-stream [input]
  (.map input (fn [e]
                 (->> (enrich (or (:drawable @events/state) (first (lookup-all (:left e) (:top e)))))
                      (merge e)))))

(defn- dispatch-events [id events]
  (let [obj (js/document.getElementById id)
        stream (merge-streams obj events)
        onstart    (.map stream (fn [e] (events/on-phase :start) e))
        normalized (.map onstart (fn [e] (normalise-event e obj)))
        delta    (delta-stream normalized (fn [acc e] {:movement-x (- (:left e) (or (:left acc) 0))
                                                       :movement-y (- (:top e) (or (:top acc) 0))}))
        enriched (enriching-stream delta)
        pattern  (.map enriched (fn [e] (events/test e)))
        last     (.map pattern  (fn [e] (merge e @events/state {:type (or (:state @events/state) (:type e))})))] ; this could be moved to events/tests at the end

      (.subscribe last  (fn [e]
                          (js/console.log (str "on " (event-name e)))
                          (b/fire (event-name e) e)))))

(defn- add-drag-start-pattern []
  (events/add-pattern :mousedrag
                      [(fn [e] (= (:type e) "mousedown"))
                       (fn [e] (= (:type e) "mousemove"))]
                      (fn [e] (enrich (:drawable e)))))

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
                        (enrich (:d (events/remove-context :mouseout))))))

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
    (dispatch-events id (clojure.string/split source-events #" "))
    (b/fire "rendering.context.update" {:canvas (:canvas data)})))

;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)]
    (t/invoke-tool tool-obj context)))
