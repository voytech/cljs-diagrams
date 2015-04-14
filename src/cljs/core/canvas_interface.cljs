(ns core.canvas-interface
  (:require [utils.dom.dom-utils :as dom]
            [tailrecursion.javelin :refer [cell]]
            [tailrecursion.hoplon :refer [canvas by-id append-child add-children! ]]
            [core.settings :refer [settings
                                   settings?
                                   settings!
                                   page-formats
                                   page-width
                                   page-height
                                   ]])
  (:require-macros [tailrecursion.javelin :refer [cell=]]
                   [core.macros :refer [with-page]]))

(declare add-item)
(declare visible-page)
(declare id2idx)
(declare idx2id)

(def project (cell {:page-index 0
                    :pages {}
                    :current-page-id "page-0"}))

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
  (let [id (get-in @project [:current-page-id])
        keyword-id (assert-keyword id)]
    (dom/console-log id)
    (get-in @project [:pages (keyword-id)])))

(defn group-elem-count [id key-group]
  (with-page (keyword id) as page
    (count (keys (get-in page [:groups key-group])))))

(defn add-item
  ([id item key]
     (.add (:canvas (proj-page-by-id id)) item)
     (swap! project assoc-in [:pages (keyword id) :buffer key] item))
  ([id item key group]
     (add-item id item key)
     (swap! project assoc-in [:pages (keyword id) :groups group (group-elem-count id group)] key)))

(defn del-item [id key]
  (with-page (keyword id) as page
    (let [elem (get-in page [:buffer key])]
      (when (not (nil? elem)) (.remove (:canvas page) elem)))))

(defn clear-group [id group-name]
  (with-page (keyword id) as page
    (let [group (get (:groups page) group-name)]
      (doseq [key (keys group)]
        (del-item id (get group key)))
      (swap! project assoc-in [:pages (keyword id) :groups] (dissoc (:groups page) [key])))))

(defn items-in-group [pageid group-key]
  (with-page (keyword pageid) as page
    (get (:groups page) group-key)))

(defn draw-grid [id]
    (with-page (keyword id) as page
      (when (= true @(settings? :snapping :visible))
        (loop [x 0 y 0]
          (if (<= @page-width x)
            x
            (let [line1 (js/fabric.Rect. (js-obj "left" 0
                                                 "top" y
                                                 "width" @page-width
                                                 "height" 1
                                                 "opacity" 0.1)),
                  line2 (js/fabric.Rect. (js-obj "left" x
                                                 "top" 0
                                                 "width" 1
                                                 "height" @page-height
                                                 "opacity" 0.1))]
              (doseq [line [line1 line2]]
                (set! (.-selectable line) false)
                (let [key (str "grid-" (group-elem-count (:id page) "grid"))]
                  (add-item (:id page) line key "grid")))
              (recur (+ @(settings? :snapping :interval) x) (+ @(settings? :snapping :interval) y))))))))


;;Not yet used. NEED TESTING.
(defn reactive-grid [settings]
   (loop [indx 0 offset 0]
           (if (< indx (group-count "grid"))
             (let [key (get (group? "grid") indx),
                   key2 (get (group? "grid") (inc indx))]
               (let [elem (get @canvas-buffer key),
                     elem2 (get @canvas-buffer key2)]
                 (dom/console-log (str key " , " key2))
                 (.setTop elem offset)
               ;;(.set elem "width" (page-width))
                 (.setLeft elem2 offset)
               ;;(.set elem2 "height" (page-height))
                 (.setVisible elem (get-in settings [:snapping :visible]))
                 (.setVisible elem2 (get-in settings [:snapping :visible])))
               (recur (+ 2 indx) (+ (get-in settings [:snapping :interval]) offset)))
             indx ))
)

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

(defn initialize-page [domid]
  (dom/wait-on-element domid (fn [id]
                               (dom/console-log (str "Initializing canvas with id [ " id " ]."))
                               (proj-create-page id)
                               (cell= (.setDimensions (:canvas (proj-page-by-id id))
                                                                (js-obj "width"  page-width
                                                                        "height" page-height)
                                                                (js-obj "cssOnly" true)))
                               (draw-grid id)
                               (.on (:canvas (proj-page-by-id id)) (js-obj "object:moving" #(do-snap %))))))

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

;; Test manage-pages-2 instead of manage-pages it looks better :)
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

(defmulti add-image :type)

(defmethod add-image "dom" [data]
  (let [photo-node (js/fabric.Image.
                           (:data data)
                           (js-obj "left"(:left (:params data))
                                   "top" (:top  (:params data))
                                   "angle"   0
                                   "opacity" 1))]
    (.add (:canvas (proj-selected-page)) photo-node)))

(defmethod add-image "raw" [data])

(defmethod add-image "url" [data])
