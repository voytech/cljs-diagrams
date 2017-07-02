(ns core.toolctors-macros)

(defmacro toolctor [& body]
  `(do ~@body))

(defmacro pipe-toolctor
  "Having a tooctor context (may be entity or entity creation context)
   and body expressions, constructs a pipeable toolctor handler. The body
   expressions are evaluated with the current context bound. Macro consumer
   has also reference for next toolctor in the invocation chain and is responsible
   to call it with result of current toolctor as context (should be entity)"
 [tool-ctx next-toolctor & body]
 `(fn [next-ctor#]
   (fn [ctx#]
     (let [~tool-ctx ctx#
           ~next-toolctor next-ctor#]
       (do ~@body)))))
