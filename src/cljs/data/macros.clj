(ns data.macros
  (:require [cljs.analyzer :as a]
            ))

(defn is-getter [fname]
  (or (not (nil? (re-find #"^get.+" fname)))
      (not (nil? (re-find #"^is.+" fname))))
)
(defn is-setter [fname]
  (or (not (nil? (re-find #"^set.+" fname))))
)

(defn- lower-first [strd]
  (let [s (seq strd)]
    (apply str (clojure.string/lower-case (first s)) (rest s))))

(defn to-property [fname]
 (if (or (is-setter fname)
         (is-getter fname))
   (-> fname
       (clojure.string/replace #"get" "")
       (clojure.string/replace #"set" "")
       (clojure.string/replace #"is" "")
       (lower-first))
   false)
)

(defmacro ->js [function-symbol jscell & args]
   (let [function-str (name function-symbol)
         prop (to-property function-str)]
     (cond (= true (is-setter function-str))
             `(data.js-cell/set ~jscell ~prop ~@args)
           (= true (is-getter function-str))
             `(data.js-cell/get ~jscell ~prop)
           )))
