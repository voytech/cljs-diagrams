(ns core.utils.dom
  (:require [cljsjs.jquery]))

(def debug false)

(defn by-id [id] (.getElementById js/document id))

(defn console-log [obj] (.log js/window.console obj))

(defn console-log-clj [obj] (.log js/window.console (.stringify js/JSON (clj->js obj))))

(defn wait-on-element [id callback]
  (if (nil? (by-id id))
    (.setTimeout js/window wait-on-element 50 id callback)
    ((fn []
        (callback id)))))

(defn do-after [callback milis]
  (.setTimeout js/window callback milis))

(defn console-log-cond [obj] (when (true? debug) (console-log obj)))

(defn time-now []
  (js/Date.))

(defn j-query [elem]
  (js/jQuery elem))

(defn j-query-id [id]
  (js/jQuery (str "#" id)))

(defn j-query-attr [attr val]
  (js/jQuery (str "[" attr "=\"" val "\"]")))

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

(defn visible [id]
  (.css (js/jQuery (str "#" id)) "display" "block"))

(defn hidden [id]
  (.css (js/jQuery (str "#" id)) "display" "none"))

(defn elem-visible [elem]
  (.css (js/jQuery elem) "display" "block"))

(defn elem-hidden [elem]
  (.css (js/jQuery elem) "display" "none"))

(defn remove-childs [parent]
  (.empty (j-query parent)))

(defn append-child [parent child]
  (.appendChild parent child))

(defn create-node [tag id]
  (let [element (.createElement js/document tag)]
    (-> element
        (attr "id" id))
    element))

(defn attr [elem name val]
  (.attr (j-query elem) name val))

(defn attrs [elem attrs]
  (when-let [target (j-query elem)]
    (doseq [key (keys attrs)]
      (.attr target
              (clj->js key)
              (clj->js (get attrs key))))))

(defn replace-container-content [container content]
  (remove-childs container)
  (append-child container content))
