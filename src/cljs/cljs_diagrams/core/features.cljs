(ns cljs-diagrams.core.features)

(defn- all-components-to [target what]
  (->> target
       :components
       (vals)
       (map what)
       (set)))

(defn _OR_ [arg1 arg2] (or arg1 arg2))
(defn _AND_ [arg1 arg2] (and arg1 arg2))

(defn- all-components-to-names [target]
  (all-components-to target :name))

(defn- all-components-to-types [target]
  (all-components-to target :type))

;; feature API:

(defn check-features [features target]
  (reduce _AND_ true (mapv #(% target) features)))

(defn any-of-features [features target]
  (reduce _OR_ false (mapv #(% target) features)))

(defn has-components-of-types [component-types]
  (fn [target]
    (= component-types (clojure.set/intersection component-types (all-components-to-types target)))))

(defn has-by-one-component-of-types [component-types]
  (fn [target]
    (let [freqs (frequencies (all-components-to target :type))]
        (->> (mapv #(get freqs %) component-types)
             (reduce +)
             (== (count component-types))))))

(defn has-only-components-of-types [component-types]
  (fn [target]
    (= component-types (all-components-to-types target))))


(defn has-components-with-names [component-names])

(defn has-only-components-with-names [component-names])

(defn has-number-of-components [target component-type expected-count])

(defn has-component-with-properties [target component-type properties])
