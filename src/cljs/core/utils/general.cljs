(ns core.utils.general)

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))
