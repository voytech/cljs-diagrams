(ns core.utils.general)

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))

(defn make-js-property
  ([jsobj property value options]
   (.defineProperty js/Object jsobj property  (js-obj "value" value
                                                       "writable"     (:writable options)
                                                       "configurable" (:configurable options)
                                                       "enumerable"   (:enumerable options))))
  ([jsobj property value]
   (make-js-property jsobj property value {:writable true
                                             :configurable true
                                             :enumerable true})))
