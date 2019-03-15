(ns cljs-diagrams.core.features)

(defn- all-shapes-to [target what]
  (->> target
       :shapes
       (vals)
       (map what)
       (set)))

(defn _OR_ [arg1 arg2] (or arg1 arg2))
(defn _AND_ [arg1 arg2] (and arg1 arg2))

(defn- all-shapes-to-names [target]
  (all-shapes-to target :name))

(defn- all-shapes-to-types [target]
  (all-shapes-to target :type))

;; feature API:

(defn check-features [features target]
  (reduce _AND_ true (mapv #(% target) features)))

(defn any-of-features [features target]
  (reduce _OR_ false (mapv #(% target) features)))

(defn has-shapes-of-types [shapes-types]
  (fn [target]
    (= shapes-types (clojure.set/intersection shapes-types (all-shapes-to-types target)))))

(defn has-by-one-shape-of-types [shapes-types]
  (fn [target]
    (let [freqs (frequencies (all-shapes-to target :type))]
        (->> (mapv #(get freqs %) shapes-types)
             (reduce +)
             (== (count shapes-types))))))

(defn has-only-shapes-of-types [shapes-types]
  (fn [target]
    (= shapes-types (all-shapes-to-types target))))


(defn has-shapes-with-names [component-names])

(defn has-only-shapes-with-names [component-names])

(defn has-number-of-shapes [target component-type expected-count])

(defn has-shape-with-properties [target component-type properties])
