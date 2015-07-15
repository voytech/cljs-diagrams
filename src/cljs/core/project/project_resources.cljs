(ns core.project.project-resources
  (:require  [tailrecursion.javelin :refer [cell]]
             [tailrecursion.hoplon :refer [canvas div $text by-id append-child add-children!]]))

(def resources (cell {}))

(def PHOTO :photo)
(def CLIPART :clipart)
(def BACKGROUND :background)

(defn add-resource [type resource]
  (println (str "Adding resource of type:" type " : " (.stringify js/JSON (clj->js resource))))
  (swap! resources assoc-in [type (:name resource)] resource))

(defn get-resource [type name]
  (let [resource-map (type @resources)]
    (when (not (nil? resource-map))
      (get resource-map name))))

(defn get-resources [type]
   )

; very fast and bad impl.
(defn find-resource
  ([name]
     (let [result (atom false)]
       (doseq [seqs (vals @resources)]
         (let [res (get seqs name)]
           (reset! result res)))
       @result))
  ([name type]
     (get-resource type name)))
