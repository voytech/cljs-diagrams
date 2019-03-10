(ns cljs-diagrams.core.funcreg)

(def funcs (volatile! {}))

(defn provider [name func]
  (vswap! funcs assoc name func))

(defn serialize [func-name & args]
  {:f func-name :args args})

(defn provide [{:keys [f args]}]
  (apply (get @funcs f) args))

(defn invoke [{:keys [f args] :as provider} & invocation-args]
  (apply (provide provider) invocation-args))