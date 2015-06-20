(ns core.canvas-interface
  (:require [utils.dom.dom-utils :as dom]
            [utils.dom.dnd-utils :as dnd]
            [tailrecursion.javelin :refer [cell]]
            [tailrecursion.hoplon :refer [canvas div $text by-id append-child add-children! ]]
            [core.actions :refer [on]]
            [data.js-cell :as jscell]
            [ui.components.popup :as p]
            [utils.popups :as popups]
            [core.entities.entity :as e]
            [core.tools.tool :as t]
            [core.actions-impl :as ac]
            [core.settings :refer [settings
                                   settings?
                                   settings!
                                   page-formats
                                   page-width
                                   page-height
                                   zoom-page-height
                                   zoom-page-width
                                   zoom
                                   ]])
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]
                   [core.macros :refer [with-page
                                        with-current-canvas]]))

(declare add-item)
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

(def obj-editing (atom false))
(def project (cell {:page-index 0
                    :pages {}
                    :current-page-id :page-0}))

(def last-change (cell 0))

(def selection_ (cell (e/create-entity "empty" (js/Object.))))
(def new_ (cell (e/create-entity "empty" (js/Object.))))

(def event-handlers (atom {"object:moving"   [#(do-snap %)
                                              #(intersection-test % "collide")
                                              #(obj-editing-start % [:left :top])]
                           "object:rotating" [#(obj-editing-start % [:angle])]
                           "object:scaling"  [#(obj-editing-start % [:scaleX :scaleY])]
                           "object:selected" [#(obj-selected %)]
                           "mouse:up" [#(mouse-up %) #(intersection-test % "collide-end")]}))

(defn- changed [] (reset! last-change (dom/time-now)))

;;Business events handlers
(defmethod on :change-page-action [action]
  (swap! project assoc-in [:page-index] (:payload action)))

(defmethod on :change-property-action [action]
  (let [payload (:payload action)
        entity  (e/entity-by-id (:entity-id payload))
        execute (:execute payload)]
    (when (= true execute)
          (e/prop-set entity (:key payload) (:value payload))
          (.renderAll (:canvas (proj-selected-page))))
    ))

(defmethod on :change-properties-action [action]
  (let [payload (:payload action)
        entity  (e/entity-by-id (:entity-id payload))
        execute (:execute payload)]
    (when (= true execute)
      (dosync
       (let [values (:values payload)]
         (doseq [key (keys values)]
           (e/prop-set entity key (key values)))))
      (.renderAll (:canvas (proj-selected-page))))
    ))




(defn proj-create-page [id]
  (let [page {:canvas (js/fabric.Canvas. id)
              :buffer {}
              :groups {}
              :id id }]
    (swap! project assoc-in [:pages (keyword id)] page)))

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

(defn snap! [target pos-prop pos-prop-set direction]
  (let  [div  (quot (pos-prop target) (:interval (:snapping @settings))),
         rest (mod  (pos-prop target) (:interval (:snapping @settings)))]
    (let [neww (* div (:interval (:snapping @settings)))]
      (when (< rest (:attract (:snapping @settings))) (pos-prop-set target neww)))))

(defn do-snap [event]
  (changed)
  (when (= true (:enabled (:snapping @settings)))
    (let [target (.-target event)]
      (snap! target #(.-left %) #(set! (.-left %) %2) 1)
      (snap! target #(.-top  %) #(set! (.-top %)  %2) 1)))
)

(defn intersection-test [event funcname]
  (let [trg (.-target event)]
    (when (not (nil? trg))
      (with-current-canvas as canvas
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
                                 (.renderAll canvas)
)))))
      )))

(defn- obj-editing-start [event properties]
  (let [trg (.-target event)
        id (e/js-obj-id trg)]
    (when (not (= true @obj-editing))
      (ac/change-properties! id (ac/build-property-map id properties) false)
      (reset! obj-editing  true)))
)

(defn- obj-editing-end []
  (let [id (:uid @selection_)]
    (reset! obj-editing false)))

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
(defn- mouse-up [event]
  (println "mouse - up ")
   (popups/hide-all)
   (obj-editing-end)
   (e/refresh @selection_))


(defn- obj-selected [event]
 (let [target (.-target event)
       entity (e/entity-from-src target)]
    (e/refresh @selection_)
    (reset! selection_ entity)
))

(defn- handle-delegator [key]
  (fn [event]
    (let [vec (get @event-handlers key)]
      (doseq [func vec]
        (func event)))))

(defn- reg-delegator [id]
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

(defn initialize-page [domid]
  (dom/wait-on-element domid (fn [id]
                               (dom/console-log (str "Initializing canvas with id [ " id " ]."))
                               (proj-create-page id)
                               (let [canvas (:canvas (proj-page-by-id id))]
                                 (cell= (do (.setWidth canvas zoom-page-width)
                                            (.setHeight canvas zoom-page-height)))
                                 (cell= (.setZoom canvas zoom))
                                 )
                               (reg-delegator id)
                               )))

(defn dispose-page [domid]

)

(defn create-page [id]
  (when (nil? (by-id id))
     (let [new (canvas :id id
                       :class "canvas")]
       (let [wrapper (by-id "canvas-wrapper")]
         (append-child wrapper new)
         (initialize-page id)))))

(defn remove-page [id]
  (when (not (nil? (by-id id)))
    (dispose-page id)
    (dom/remove-element (dom/parent (by-id id)))))

(defn select-page [index]
  (let [id (idx2id index)]
    (if (not (= (get-in @project [:current-page-id]) id))
      (do
        (dom/console-log (str "selecting page :" index ", id :" id))
        (swap! project assoc-in [:current-page-id] id)
        (visible-page id)
        true)
      false)))

(defn- visible-page [id]
  (.css (js/jQuery ".canvas-container") "display" "none")
  (.css (.parent (js/jQuery (str "#" id))) "display" "block"))

(defn- paging-states-diff [settings]
  (let [dom-pages-cnt    (dom/children-count (by-id "canvas-wrapper"))
        proj-pages-cnt   (get-in settings [:pages :count])
        multi-page       (get-in settings [:multi-page])
        target-num       (if multi-page proj-pages-cnt 1)]
    {:differs (not (= dom-pages-cnt target-num))
     :actual-num dom-pages-cnt
     :target-num target-num
     :multi-page multi-page})
)

(defn- re-page? [{:keys [differs multi-page actual-num] :as diff}]
  (or differs (= 0 actual-num)))

(defn manage-settings [settings]
  (dom/console-log "manage-settings")
  (let [{:keys [differs actual-num target-num multi-page] :as diff} (paging-states-diff settings)]
    (when (re-page? diff)
        (dom/console-log "re-paging...")
        (let [orphans-count    (- actual-num target-num)
              orphans-index    (- actual-num orphans-count)
              max-cnt          (max actual-num target-num)]

           (doall (map #(cond (< % orphans-index) (create-page (page-id %))
                              (>= % orphans-index) (remove-page (page-id %))) (range 0 max-cnt))))
        (if (not  (select-page (get-in @project [:page-index])))
                  (visible-page (get-in @project [:current-page-id]))))))

(defn initialize-workspace []
  (cell= (manage-settings settings))
  (cell= (select-page (get-in project [:page-index])))
)

;;--------------------------------
;; API dnd event handling with dispatching on transfer type
;;---------------------------------

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-name tool)]
    (println (str "Invoking action " tool context))
    ((:func-ctor tool-obj) tool-obj context))
)

(defmethod dnd/dispatch-drop-event "imgid" [event]
  {:data (dom/by-id (dnd/get-dnd-data event "imgid"))
   :params (dnd/event-layer-coords event)
   :type "dom"}
)

(defmethod dnd/dispatch-drop-event "text/html" [event]
 {:data (dnd/get-dnd-data event "text/html")
  :params (dnd/event-layer-coords event)
  :type "dom"}
)

;;
;;API methods !
;;

(defn add-entity [entity]
  (when (not (instance? e/Entity entity))
    (throw (js/Error. (str entity " is not an core.entities.Entity object"))))
  (let [src (:src entity)]
    (if (not (nil? src))
      (do
        (.add (:canvas (proj-selected-page)) src)
        (e/refresh entity)
        (reset! new_ entity)
        (changed))
        ))
)
