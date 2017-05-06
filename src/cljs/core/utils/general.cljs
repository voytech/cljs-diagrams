(ns core.utils.general
  (:require [cljs-uuid-utils.core :as u]))

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))

(defn uuid []
  (-> u/make-random-uuid u/uuid-string))
