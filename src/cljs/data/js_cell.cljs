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

(defprotocol IJsCell
  (bind [this mjsobj])
  (get [this property])
  (set [this property val]))

(deftype JsCell [cel ^{:volatile-mutable true} jsobj handler]
  IJsCell
  (bind [this mjsobj]
     (set! jsobj mjsobj)
     (dosync
      (reset! cel {})
      (doall (mapv #(swap! cel assoc-in [(keyword %)] (goog.object/get jsobj %)) (properties jsobj))))
    )

  (get [this property]
    (cell= (get-in cel [(keyword property)])))

  (set [this property val]
    (println (str property " : " val))
    (goog.object/set jsobj property val)
    (when (not (nil? handler)) (handler jsobj property val))
    (swap! cel assoc-in [(keyword property)] val)
    ))

(defn js-cell
  ([jsobj] (JsCell. (cell {}) jsobj nil))
  ([jsobj handler] (JsCell. (cell {}) jsobj handler))
  ([] (js-cell (js/Object.))))
