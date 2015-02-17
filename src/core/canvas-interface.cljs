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
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(declare add-item)
(declare visible-page)

(defn js-conj [jscontainer obj]
  (.add jscontainer obj)
  jscontainer
)

(defn js-rem [jscontainer obj]
  (.remove jscontainer obj)
  jscontainer
)

(def node-keys-group (atom {}))
(def canvas-buffer (atom {}))
(def canvas-sheet (atom ))

(def page {:groups node-keys-group
           :buffer canvas-buffer
           :canvas canvas-sheet
           :index 0
           :id "page-1"})

(def project (cell {:current-page-idx 0
                    :pages {}
                    :current-page {}}))

(defn add-item
  ([item key]
     (swap! canvas-sheet js-conj item)
     (swap! canvas-buffer assoc key item))
  ([item key group]
     (add-item item key)
     (swap! node-keys-group assoc-in [group (count (keys (get @node-keys-group group)))] key)))

(defn del-item [key]
  (let [elem (get @canvas-buffer key)]
    (when (not (nil? elem)) (swap! canvas-sheet js-rem elem))))

(defn del-items [key-group]
  (let [key-map (get @node-keys-group key-group)]
    (doseq [key (keys key-map)]
      (del-item (get key-map key))))
   (swap! node-keys-group dissoc group))

(defn group-count [key-group]
  (let [key-map (get @node-keys-group key-group)]
    (count (keys key-map))))

(defn group? [key-group]
   (get @node-keys-group key-group)
)

(defn draw-grid []
  (del-items "grid")
  (when (= true @(settings? :snapping :visible))
    (loop [x 0 y 0]
      (if (<= (.getWidth @canvas-sheet) x)
        x
        (let [line1 (js/fabric.Rect. (js-obj "left" 0
                                             "top" y
                                             "width" (.getWidth @canvas-sheet)
                                             "height" 1
                                             "opacity" 0.1)),
              line2 (js/fabric.Rect. (js-obj "left" x
                                             "top" 0
                                             "width" 1
                                             "height" (.getHeight @canvas-sheet)
                                             "opacity" 0.1))]
          (doseq [line [line1 line2]]
            (set! (.-selectable line) false)
            (let [key (str "grid-" (group-count "grid"))]
              (add-item line key "grid")))
          (recur (+ @(settings? :snapping :interval) x) (+ @(settings? :snapping :interval) y))))))
)

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

(defn initialize-page [domid]
  (dom/wait-on-element domid (fn [id]
                               (dom/console-log (str "Initializing canvas with id [ " id " ]."))
                               (reset! canvas-sheet (js/fabric.Canvas. id ))
                               (visible-page "page-0")
                               (cell= (.setDimensions @canvas-sheet (js-obj "width"  page-width
                                                                            "height" page-height)
                                                             (js-obj "cssOnly" true)))
                               (draw-grid)
                             ;;(cell= (reactive-grid))
                               (.on @canvas-sheet (js-obj "object:moving"
                                                            #(do-snap %))))))

(defn page-id [indx]
  (str "page-" indx))

(defn create-page [id]
  (dom/console-log "creating page DOM element!")
  (when (nil? (by-id id))
     (let [new (canvas :id id
                       :class  "canvas")]
       (dom/console-log new)
       (let [wrapper (by-id "canvas-wrapper")]
         (dom/console-log wrapper)
         (append-child wrapper new)
         (initialize-page id)))))

(defn select-page [id]
  (dom/swap-childs (child-at (dom/j-query-id "canvas-wrapper") (page-id 0))
                   (child-at (dom/j-query-id "canvas-wrapper") id))
  (visible-page id))


(defn visible-page [id]
  (.css (js/jQuery ".canvas-container") "display" "none")
  (.css (.parent (js/jQuery (str "#" id))) "display" "block")
)

(defn manage-pages [settings]
  (cond
    (true? (get-in settings [:multi-page]))
       (loop [indx 0]
               (if (< indx (get-in settings [:pages :count]))
                 (do (create-page (page-id indx))
                     (recur (inc indx)))
                 true)
               )
       :else (create-page (page-id 0))))

(defn initialize-workspace []
  (cell= (manage-pages settings))) ;;IMPORTANT NOTE A CELL MUST BE USED IN SAME LEXICAL SCOPE AS FORMULA.

(defmulti add :type)

(defmethod add "dom" [data]
 (let [photo-node (js/fabric.Image.
                           (:data data)
                           (js-obj "left"(:left (:params data))
                                   "top" (:top  (:params data))
                                   "angle"   0
                                   "opacity" 1))]
    (swap! canvas-sheet js-conj photo-node)))

(defmethod add "raw" [data])

(defmethod add "url" [data])
