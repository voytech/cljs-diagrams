(ns core.canvas-interface
  (:require [utils.dom.dom-utils :as dom]
            [tailrecursion.javelin :refer [cell]]
            [tailrecursion.hoplon :refer [canvas div $text by-id append-child add-children! ]]
            [core.actions :refer [on]]
            [data.js-cell :as jscell]
            [ui.components.popup :as p]
            [core.settings :refer [settings
                                   settings?
                                   settings!
                                   page-formats
                                   page-width
                                   page-height
                                   ]])
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]
                   [core.macros :refer [with-page]]))

(declare add-item)
(declare visible-page)
(declare id2idx)
(declare idx2id)
(declare proj-page-by-id)
(declare proj-selected-page)

(def project (cell {:page-index 0
                    :pages {}
                    :current-page-id :page-0}))


(def selection_ (jscell/js-cell (js/Object.) (fn [obj prop val]
                                              ;; (.setActiveObject (:canvas (proj-selected-page)) obj)
                                               (.setCoords obj)
                                               (.renderAll (:canvas (proj-selected-page)) true)
                                               )))
(def popups (atom ()))

;;Business events handlers
(defmethod on :change-page-action [action]
  (swap! project assoc-in [:page-index] (:payload action)))


(defn proj-create-page [id]
  (let [page {:canvas (js/fabric.Canvas. id)
              :buffer {}
              :groups {}
              :index index
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

(defn selected-object []
   (with-current-page as page
       (let [canvas (:canvas page)
             active (.getActiveObject canvas)]
       )))

(defn selected-obj-property [prop]
  (jscell/get selection_ prop))


;; (defn group-elem-count [id key-group]
;;   (with-page (keyword id) as page
;;     (count (keys (get-in page [:groups key-group])))))

;; (defn add-item

;;   ([id item key]
;;      (.add (:canvas (proj-page-by-id id)) item)
;;      (swap! project assoc-in [:pages (keyword id) :buffer key] item))
;;   ([id item key group]
;;      (add-item id item key)
;;      (swap! project assoc-in [:pages (keyword id) :groups group (group-elem-count id group)] key)))

;; (defn del-item [id key]
;;   (with-page (keyword id) as page
;;     (let [elem (get-in page [:buffer key])]
;;       (when (not (nil? elem)) (.remove (:canvas page) elem)))))

;; (defn clear-group [id group-name]
;;   (with-page (keyword id) as page
;;     (let [group (get (:groups page) group-name)]
;;       (doseq [key (keys group)]
;;         (del-item id (get group key)))
;;       (swap! project assoc-in [:pages (keyword id) :groups] (dissoc (:groups page) [key])))))

;; (defn items-in-group [pageid group-key]
;;   (with-page (keyword pageid) as page
;;     (get (:groups page) group-key)))

(defn snap! [target pos-prop pos-prop-set direction]
  (let  [div  (quot (pos-prop target) (:interval (:snapping @settings))),
         rest (mod  (pos-prop target) (:interval (:snapping @settings)))]
    (let [neww (* div (:interval (:snapping @settings)))]
      (when (< rest (:attract (:snapping @settings))) (pos-prop-set target neww)))))

(defn do-snap [event]
  (when (= true (:enabled (:snapping @settings)))
    (let [target (.-target event)]
      (snap! target #(.-left %) #(set! (.-left %) %2) 1)
      (snap! target #(.-top  %) #(set! (.-top %)  %2) 1)))
)

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

(defn- obj-selected [event]
 (let [target (.-target event)]
   (jscell/bind selection_ target)))

(defn- obj-modified [event]
  (let [target (.-target event)]))

(defn- page-event-handlers [id handlers]
  (doseq [entry handlers]
    (.on (:canvas (proj-page-by-id id)) (js-obj (first entry) (last entry)))))

(defn- event-coords [event]
  {:x (.-clientX event)
   :y (.-clientY event)})

(defn- handle-popups [event]
  (let [cords (event-coords event)]
    (println (count @popups))
    (doseq [popup @popups]
      (p/attach popup "popups-holder")
      (p/show-at popup (:x cords) (:y cords)))
    ))

(defn initialize-page [domid]
  (dom/wait-on-element domid (fn [id]
                               (dom/console-log (str "Initializing canvas with id [ " id " ]."))
                               (proj-create-page id)
                               (cell= (.setDimensions (:canvas (proj-page-by-id id))
                                                                (js-obj "width"  page-width
                                                                        "height" page-height)
                                                                (js-obj "cssOnly" true)))
                               (page-event-handlers id [["object:moving"   #(do-snap %)]
                                                        ["object:selected" #(obj-selected %)]
                                                        ["object:modified" #(obj-modified %)]
                                                        ["mouse:down" #(handle-popups (.-e %))]])
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


;;
;;API methods !
;;
(defn add-popup [id popup]
  (println (str "Adding popup..." id popup))
  (swap! popups conj (p/default-popup id popup)))

(defmulti add-image :type)

(defmethod add-image "dom" [data]
  (println (:data data))
  (let [photo-node (js/fabric.Image.
                           (:data data)
                           (js-obj "left"(:left (:params data))
                                   "top" (:top  (:params data))
                                   "angle"   0
                                   "opacity" 1))]
    (.add (:canvas (proj-selected-page)) photo-node)))

(defmethod add-image "raw" [data])

(defmethod add-image "url" [data])
