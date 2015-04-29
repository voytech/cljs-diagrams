(ns data.js-cell
  (:require [tailrecursion.javelin :refer [cell]])
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]))

;; definition.
;; (def entity (js-cell))
;; (bind entity javascript-object)

;; reactive updates
;; (> getOpacity entity)   -- a macro coverting symbol into call to javascript
;; (> getXPos entity)

;; writing values
;; (> setOpacity entity 0.5)
;;
;; TODO: make javascript inner object observalble and register property observer so that changed property is reflected within proxy js-cell
;; TODO: make ability to change nested properties by key path e.g. [:position :left]
;; TODO: make property filter so that we can observe and modify via proxy only given properites - includes and excludes rules.
(defn is-getter [fname]
  (or (not (nil? (re-find #"^get.+" fname)))
      (not (nil? (re-find #"^is.+" fname))))
)
(defn is-setter [fname]
  (or (not (nil? (re-find #"^set.+" fname))))
)


(defn to-property [fname]
 (if (or (is-setter fname)
         (is-getter fname))
   (-> fname
       (clojure.string/replace #"get" "")
       (clojure.string/replace #"set" "")
       (clojure.string/replace #"is" ""))
   false)
)

(defn properties [jsobj]
  (let [props (atom [])]
    (goog.object/forEach jsobj
                         (fn [val key obj]
                           (when (not (= (type val) js/Function))
                             (swap! props conj key))))
    @props))

(defn- update-js [cel jsobj handler]
  (when (map? @cel)
    (doseq [property (keys @cel)]
      (cell= (do
                ;; (println (str "property :" property " has value: " (get-in cel [property]) ))
                 (->> (get-in cel [property])
                      (goog.object/set jsobj (name property)))
                 ;; (when (not (nil? handler))
                 ;;   (handler jsobj (name property) (get-in cel [property])))
                 )
             ))))

(defprotocol IJsCell
  (bind [this mjsobj])
  (data [this])
  (get [this property])
  (set [this property val]))

(deftype JsCell [cel ^{:volatile-mutable true} jsobj handler]
  IJsCell
  (bind [this mjsobj]
     (set! jsobj mjsobj)
     (dosync
      (reset! cel {})
      (doall (mapv #(swap! cel assoc-in [(keyword %)] (goog.object/get jsobj %)) (properties jsobj))))
     (update-js cel jsobj handler)
    )
  (data [this] @cel)
  (get [this property]
    (cell= (get-in cel [(keyword property)])))

  (set [this property val]
   ;; (goog.object/set jsobj property val)
   ;; (when (not (nil? handler)) (handler jsobj property val)) ;; this line and above in func put into cell= on all props
    (swap! cel assoc-in [(keyword property)] val)  ;; keep only these
    ))

(defn js-cell
  ([jsobj] (JsCell. (cell {}) jsobj nil))
  ([jsobj handler] (JsCell. (cell {}) jsobj handler))
  ([] (js-cell (js/Object.))))
