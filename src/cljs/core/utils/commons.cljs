(ns core.utils.commons)

(defn makeRefs [container]
  (conj (:parentsRefs container) {(type container)  (:uid container)})

(defn refs [container]
  (or (:parentsRefs container) []))
