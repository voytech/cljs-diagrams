(ns data.js-cell
  (:require [tailrecursion.javelin :refer [cell destroy-cell!]])
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
;move do sync to refresh
(defn properties [jsobj func]
  (let [props (atom [])]
     (goog.object/forEach jsobj
                          (fn [val key obj]
                            (when (not (= (type val) js/Function))
                              (when (= "top" key) (println (str "p " key " v " val)))
                              (func key val)
                              (swap! props conj key))))
    @props))

(defn- update-js [cel formula-store jsobj handler]
  (doseq [formula @formula-store]
    (destroy-cell! formula))
  (reset! formula-store [])
  (when (map? @cel)
    (doseq [property (keys @cel)]
      (swap! formula-store conj (cell= (do
                                         (when (not (nil? (get-in cel [property])))
                                           (->> (get-in cel [property])
                                                (goog.object/set jsobj (name property)))
                                           (when (= :top property) (println (str "->" (name property) ":" (get-in cel [property]))))))
                                       )))))

(defprotocol IJsCell
  (refresh [this])
  (bind [this mjsobj])
  (data [this])
  (get [this property])
  (set [this property val]))

(deftype JsCell [cel formula-store ^{:volatile-mutable true} jsobj handler]
  IJsCell
  (refresh [this]
    (dosync (properties jsobj (fn [key val] (swap! cel assoc-in [(keyword key)] val)))))

  (bind [this mjsobj]
     (set! jsobj mjsobj)
     (dosync
      (reset! cel {})
      (properties jsobj (fn [key val] (swap! cel assoc-in [(keyword key)] val)))
      (update-js cel formula-store jsobj handler)))

  (data [this] @cel)

  (get [this property]
    (cell= (get-in cel [(keyword property)])))

  (set [this property val]
    (swap! cel assoc-in [(keyword property)] val)  ;; keep only these
    (println (str "setting value :" (property) " = " ((keyword property) @cel)))))

(defn js-cell
  ([jsobj]
     (let [jscell (JsCell. (cell {}) (atom []) jsobj nil)]
       (bind jscell jsobj)
       jscell))
  ([jsobj handler] (JsCell. (cell {}) (atom []) jsobj handler))
  ([] (js-cell (js/Object.))))
