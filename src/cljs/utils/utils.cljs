(ns utils.utils
  (:require [utils.dom.dom-utils :as dom]))

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))
