(ns core.project
 (:require [reagent.core :as reagent :refer [atom]]
           [cljsjs.jquery]
           [cljsjs.fabric]
           [core.utils.dom :as dom]
           [core.utils.dnd :as dnd]
           [core.entities :as e]
           [core.tools :as t]))

(:require-macros [core.macros :refer [with-page
                                      with-current-canvas]])

(declare add-item)
(declare add-entity)
(declare visible-page)
(declare id2idx)
(declare idx2id)
(declare proj-page-by-id)
(declare proj-selected-page)
(declare add-event-handler)
(declare do-snap)
(declare intersection-test)
(declare obj-selected)
(declare obj-modified)
(declare obj-editing-start)
(declare mouse-up)
(declare reg-delegator)

;;(defonce page-count (atom 4))
(defonce project (atom {:page-index 0
                        :pages {}
                        :current-page-id :page-0}))

(def last-change (atom 0))
(def obj-editing (atom false))

(def event-handlers (atom {"object:moving"   [#(do-snap %)]
                                            #(intersection-test % "collide")
                                            #(obj-editing-start % [:left :top])
                           "object:rotating" [#(obj-editing-start % [:angle])]
                           "object:scaling"  [#(obj-editing-start % [:scaleX :scaleY])]
                           "object:selected" [#(obj-selected %)]
                           "mouse:up" [#(mouse-up %) #(intersection-test % "collide-end")]}))

(defn- changed [] (reset! last-change (dom/time-now)))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defn proj-page-by-id [id]
  (let [keyword-id (assert-keyword id)]
    (get-in @project [:pages keyword-id])))

(defn proj-selected-page []
  (when (not (nil? project))
    (let [id (get-in @project [:current-page-id])
          keyword-id (assert-keyword id)]
      (get-in @project [:pages keyword-id]))))

(defn initialize-page [id {:keys [width height]}]
  (dom/console-log (str "Initializing canvas with id [ " id " ]."))
  (let [page {:canvas (js/fabric.Canvas. id)
              :id (assert-keyword id)
              :width width
              :height height}]
    (.setWidth (:canvas page) width)
    (.setHeight (:canvas page) height)
    (swap! project assoc-in [:pages (keyword id)] page))
  ;;(let [canvas (:canvas (proj-page-by-id id))]
  ;;  (do (.setWidth canvas @zoom-page-width)
  ;;      (.setHeight canvas @zoom-page-height)
  ;;  (.setZoom canvas @zoom))



  (reg-delegator id))

(defn remove-page [domid]
  (let [page (proj-page-by-id domid)
        canvas (:canvas page)]
       (.clear canvas)
       (.dispose canvas))
  (swap! project update-in [:pages] dissoc (assert domid)))

(defn add-page []
  (let [cnt (-> @project :pages keys count)
        id (keyword (str "page-" cnt))]
    (swap! project assoc-in [:pages id] {:canvas nil :id id})))

(defn select-page [maybe-raw-id]
  (let [id (assert-keyword maybe-raw-id)]
    (if (not (= (get-in @project [:current-page-id]) id))
      (do
        (swap! project assoc-in [:current-page-id] id)
        true)
      false)))

(defn cleanup-project-data []
  (doseq [page (vals (:pages @project))]
    (->> page :canvas .clear))
  (reset! project {:page-index -1
                   :pages {}
                   :current-page-id nil})
  (reset! e/entities {})
  (changed))

(defn snap! [target pos-prop pos-prop-set direction]
  (let  [div  (quot (pos-prop target) (:interval (:snapping @settings))),
         rest (mod  (pos-prop target) (:interval (:snapping @settings)))]
    (let [neww (* div (:interval (:snapping @settings)))]
      (when (< rest (:attract (:snapping @settings))) (pos-prop-set target neww)))))

(defn do-snap [event])


(defn intersection-test [event funcname]
  (let [trg (.-target event)]
    (when (not (nil? trg))
      (let [canvas (:canvas (proj-selected-page))]
        (.forEachObject canvas
                        #(when (not (== % trg))
                           (when (or (.intersectsWithObject trg %)
                                     (.isContainedWithinObject trg %)
                                     (.isContainedWithinObject % trg))
                             (let [trge (e/entity-from-src trg)
                                   inte (e/entity-from-src %)
                                   collide-trge (get (:event-handlers trge) funcname)
                                   collide-inte (get (:event-handlers inte) funcname)]
                               (cond
                                 (not (nil? collide-inte)) (collide-inte inte trge)
                                 (not (nil? collide-trge)) (collide-trge trge inte))
                               (.renderAll canvas)))))))))

(defn- obj-editing-start [event properties])

(defn- obj-editing-end [])

(defn page-id [indx]
  (str "page-" indx))

(defn idx2id
  "Function returns DOM id for given page index. It assumes that there is already
   child dom node for canvas-container at this index."
  [idx]
  (let [node (.get (dom/j-query-class "canvas-container") idx)]
     (if (not (nil? node))
       (.attr (.first (.children (dom/j-query node))) "id") -1)))

(defn id2idx [id]
  (let [c-container (dom/parent (by-id id))]
    (.index (dom/j-query-class "canvas-container") c-container)))

;;
;;Input events handlers
;;
(defn- mouse-up [event])


(defn- obj-selected [event]
  (let [target (.-target event)]
       entity (e/entity-from-src target)
    (e/refresh @selection_)
    (reset! selection_ entity)))


(defn- handle-delegator [key]
  (fn [event]
    (let [vec (get @event-handlers key)]
      (doseq [func vec]
        (func event)))))

(defn- reg-delegator [id]
  (let [page (:canvas (proj-page-by-id id))]
    (.on page (js-obj "object:scaling" (fn [e]
                                        (let [obj (.-target e)
                                              strokeWidth (/ (.-strokeWidth obj) (/ (+ (.-scaleX obj) (.-scaleY obj)) 2))
                                              activeObj (.getActiveObject page)]
                                           (.set activeObj "strokeWidth" 2))))))
  (doall
   (map #(.on (:canvas (proj-page-by-id id)) (js-obj % (handle-delegator %)))
         ["object:moving"
          "object:rotating"
          "object:scaling"
          "object:selected"
          "object:modified"
          "mouse:down"
          "mouse:up"])))

(defn- page-event-handlers [id handlers]
  (doseq [entry handlers]
      (.on (:canvas (proj-page-by-id id)) (js-obj (first entry) (last entry)))))

(defn- event-coords [event]
  {:x (.-clientX event)
   :y (.-clientY event)})

(defn add-event-handler [event func]
  (let [vec (get @event-handlers event)]
    (if (nil? vec) (swap! event-handlers assoc-in [event] (vector func))
                   (swap! event-handlers assoc-in [event (count vec)] func))))


;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)
        entity (t/invoke-tool tool-obj context)]
      (add-entity entity)))


(defmethod dnd/dispatch-drop-event "imgid" [event]
  {:data (dom/by-id (dnd/get-dnd-data event "imgid"))
   :params (dnd/event-layer-coords event)
   :type "dom"})

(defmethod dnd/dispatch-drop-event "text/html" [event]
  {:data (dnd/get-dnd-data event "text/html")}
  :params (dnd/event-layer-coords event)
  :type "dom")

;;
;;API methods !
;;It can be re-factored so that each
;;entity is added via add-entity multi method.
;;A dispatch then should be made on entity type.
;;

(defn- enrich-handler [handler]
  (fn [e]
    (let [jsobj (.-target e)
          part (.-refPartId jsobj)
          entity (e/entity-from-src jsobj)]
      (handler {:src jsobj :entity entity :part part :event e}))))

(defn- register-handlers [canvas part]
  (doseq [key (keys (:event-handlers part))]
    (let [handler (get (:event-handlers part) key)]
      (.on canvas (js-obj key (enrich-handler handler))))))

(defn add-entity [entity]
  (when (not (instance? e/Entity entity))
    (throw (js/Error. (str entity " is not an core.entities. Entity object"))))
  (let [parts (:parts entity)
        canvas (:canvas (proj-selected-page))]
    (doseq [part parts]
      (let [src (:src part)]
        (.add canvas src)
        (register-handlers canvas part)))
    (.renderAll canvas)
    (changed)))
