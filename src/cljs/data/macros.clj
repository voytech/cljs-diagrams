(ns data.macros
  (:require [cljs.analyzer :as a]))

;; (with-page :page-1 as page (do ))
(defmacro ->js [function-symbol jscell & args]
  (cond (not (symbol? function-symbol))  (throw (Exception. "first argument must be javascript setter or getter function symbol")))
  `(let [property  (data.js-cell/to-property (name ~function-symbol))
         is-setter (data.js-cell/is-setter   (name ~function-symbol))
         is-getter (data.js-cell/is-getter   (name ~function-symbol))]
     (cond (= true is-setter) (data.js-cell/set ~jscell property (first ~args) )
           (= true is-getter) (data.js-cell/get ~jscell property))))
