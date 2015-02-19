(ns utils.dom.dom-utils)

(def debug false)

(defn by-id [id] (.getElementById js/document id))

(defn console-log [obj] (.log js/window.console obj))

(defn wait-on-element [id callback]
  (if (nil? (by-id id))
    (def interval (.setInterval js/window wait-on-element 50 id callback))
    ((fn []
        (.clearInterval js/window interval)
        (callback id)))
    ))

(defn console-log-cond [obj] (when (true? debug) (console-log obj)))

(defn j-query [elem]
  (js/jQuery elem))

(defn j-query-id [id]
  (js/jQuery (str "#" id)))

(defn j-query-class [id]
  (js/jQuery (str "." id)))
(defn del-ids [head  & tail])

(defn remove-element [element]
  (.remove (j-query element)))

(defn children-count [element]
  (.-length (.children (j-query element))))

(defn hide-childs [parent])

(defn visible-child [parent child])

(defn parent [child]
  (.parent (j-query child)))

(defn child-at [parent index]
  (.get (j-query parent) index))

(defn swap-childs [child1 child2]
  (.insertBefore (j-query child2) child1))

(defn swap-childs-idx [parent idx1 idx2]
  (swap-childs (child-at parent idx1)
               (child-at parent idx2)))

(defn child-index [parent child])
