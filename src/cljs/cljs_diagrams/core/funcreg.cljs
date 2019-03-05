(ns cljs-diagrams.core.funcreg)

(def funcs (volatile! {}))

(defn reg [func]
  (let [uuid (str (random-uuid))]
    (vswap! funcs assoc uuid func)
    uuid))

(defn fun [uuid] (get @funcs uuid))