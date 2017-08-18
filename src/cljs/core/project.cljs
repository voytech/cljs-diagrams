(ns core.project
 (:require [reagent.core :as reagent :refer [atom]]
           [cljsjs.jquery]
           [cljsjs.fabric]
           [core.utils.dom :as dom]
           [core.utils.dnd :as dnd]
           [core.entities :as e]
           [core.tools :as t]
           [core.layouts :as layouts]))

(declare add-item)
(declare sync-entity)
(declare visible-page)
(declare id2idx)
(declare idx2id)
(declare proj-page-by-id)
(declare proj-selected-page)
(declare add-event-handler)
(declare obj-selected)
(declare obj-modified)
(declare obj-editing-start)
(declare mouse-up)
(declare register-handlers)

;;(defonce page-count (atom 4))
(defonce project (atom {:page-index 0
                        :pages {}
                        :current-page-id :page-0}))

(defonce event-store (atom []))

(defn add-event [event]
  (when (> (count @event-store) 20)
    (swap! event-store subvec 1))
  (swap! event-store conj event))

(defn prev-event []
  (last @event-store))

(def last-change (atom 0))
(def obj-editing (atom false))

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


(defn- enrich-event-and-handle [canvas e event-type decomposed handler]
  (let [event {:src (.-target e)
               :event e
               :x (.-layerX e)
               :y (.-layerY e)
               :movement-x (.-movementX e)
               :movement-y (.-movementY e)
               :entity (-> decomposed :entity)
               :canvas canvas
               :drawable (-> decomposed :drawable-name)
               :type event-type}]
      ;(bus/fire (str (:type entity) "." ()))
      (handler event)
      (add-event event)
      (.renderAll canvas)))

(defn- decompose [target]
  (let [drawable-name      (.-refPartId target)
        attribute-value-id (.-refAttrId target)
        entity-id          (.-refId target)
        entity             (e/entity-from-src target)
        attribute-value    (e/get-attribute-value entity attribute-value-id)]
    {:entity           entity
     :attribute-value  attribute-value
     :drawable-name    drawable-name}))

(defmulti handle-event-by-class (fn [canvas event-type e] (if (not (nil? (.-refAttrId (.-target e)))) :attribute :entity)))

(defmethod handle-event-by-class :attribute [canvas event-type e]
  (let [decomposed (decompose (.-target e))
        handler (get-in @e/attribute-events [(-> decomposed :attribute-value :attribute :name)
                                             (:type (e/get-attribute-value-drawable (:attribute-value decomposed) (:drawable-name decomposed)))
                                             event-type])]
      (when (not (nil? handler))
        (enrich-event-and-handle canvas e event-type decomposed handler))))

(defmethod handle-event-by-class :entity [canvas event-type e]
  (let [decomposed (decompose (.-target e))
        handler (get-in @e/entity-events [(-> decomposed :entity :type)
                                          (:type (e/get-entity-drawable (:entity decomposed) (:drawable-name decomposed)))
                                          event-type])]
      (when (not (nil? handler))
        (enrich-event-and-handle canvas e event-type decomposed handler))))

(defn- dispatch-events [canvas]
  (doseq [event-type ["object:moving"  "object:rotating"
                      "object:scaling" "object:selected"
                      "mouse:down"     "mouse:up"
                      "mouse:over"     "mouse:out"
                      "mouse:click"    "mouse:dbclick"]]
      (.on canvas (js-obj event-type (fn [e]
                                        (when-let [jsobj  (.-target e)]
                                          (handle-event-by-class canvas event-type e)))))))

(defn initialize-page [id {:keys [width height]}]
  (dom/console-log (str "Initializing canvas with id [ " id " ]."))
  (let [page {:canvas (js/fabric.Canvas. id)
              :id (assert-keyword id)
              :width width
              :height height}]
    (.setWidth (:canvas page) width)
    (.setHeight (:canvas page) height)
    (swap! project assoc-in [:pages (keyword id)] page)
    (dispatch-events (:canvas page))))
  ;;(let [canvas (:canvas (proj-page-by-id id))]
  ;;  (do (.setWidth canvas @zoom-page-width)
  ;;      (.setHeight canvas @zoom-page-height)
  ;;  (.setZoom canvas @zoom))



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

(defn- obj-selected [event])


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

;TODO how should we handle dragNdrop events originating from particulaar tool? If not all tools produces entities - some can have different behaviour in canvas context
; For example : attribute value producing tool will bind attrib value to entity. It in fact can just return entity to which attribute value was added
; then this entity is going to be synchronized - all changes made are going to be propageted to canvas.

(defmethod dnd/dispatch-drop-event "tool-data" [event]
  (let [tool-id (dnd/get-dnd-data event "tool-data")
        context (dnd/event-layer-coords event)
        tool-obj (t/by-id tool-id)
        entity (t/invoke-tool tool-obj context)]
      (sync-entity entity)))

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

(defn- contains [canvas src]
  (let [test (atom false)]
    (.forEachObject canvas (fn [e] (when (= e src) (reset! test true))))
    @test))

;make it generic not only for entities but for all drawable-awares (furhter partials-aware)
(defn- delete-drawable-orphans [entity]
  (let [canvas (:canvas (proj-selected-page))
        drawable-names (set (mapv #(:name %) (e/components entity)))]
    (.forEachObject canvas (fn [e]
                             (when (= (.-refId e) (:uid entity))
                               (when-not (contains? drawable-names (.-refPartId e))
                                 (.remove canvas e)))))))

;Now works only for offset mode - left and top of source is relative to bounding box.

(defn- sync-attributes [canvas entity attribute-values]
  (let [bbox (layouts/get-bbox entity)
        cbbox {:left (+ (:left (:content-bbox entity)) (:left bbox))
               :top  (+ (:top (:content-bbox entity))  (:top bbox))
               :width   (:width (:content-bbox entity))
               :height  (:height (:content-bbox entity))}]
    (doseq [attribute-value attribute-values]
      (doseq [drawable (e/components attribute-value)]
        (when-not (contains canvas (:src drawable))
          (.add canvas (:src drawable)))))
    (layouts/layout cbbox attribute-values)))

(defn sync-entity [entity]
  (when (not (instance? e/Entity entity))
    (throw (js/Error. (str entity " is not an core.entities. Entity object"))))
  (let [drawables (e/components entity)
        attributes (:attributes entity)
        canvas (:canvas (proj-selected-page))]
    (delete-drawable-orphans entity)
    (doseq [drawable drawables]
      (let [src (:src drawable)]
        (when-not (contains canvas src)
          (.add canvas src))))
    (sync-attributes canvas entity attributes)
    (.renderAll canvas)
    (changed)))
