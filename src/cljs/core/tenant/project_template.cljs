(ns core.tenant.project-template
  (:require [tailrecursion.javelin :refer [cell destroy-cell! set-cell!]]
            [core.actions :as actions])
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]))

(def ^:private DEFAULT_NAME "Enter name")
(def ^:private DEFAULT_DESCRIPTION "Enter description")

;;TODO:Cells below will be loaded from castra backend.
;;project-templates changes only when adding new template/ when paging.
(def project-templates (cell []))
(def current-template-index (atom -1))
;;current-template changes only when editing.
(def current-template (cell nil))

(defrecord ProjectTemplate [name
                            description
                            page-count
                            fixed-count?
                            max-page-count
                            page-formats
                            custom-format?
                            printing-surfaces
                            client-requests?])

(defn- index-of [coll v]
  (let [i (count (take-while #(not= v %) coll))]
    (when (or (< i (count coll))
            (= v (last coll)))
      i)))

(defn- by-name [name]
  (first (filter #(= name (:name %)) @project-templates)))

(defn- template-index-of [name]
  (index-of @project-templates (by-name name)))

(defn- next-default-name [name index]
  (let [found-name (by-name name)]
    (if (not (nil? found-name))
      (recur (str DEFAULT_NAME " " index) (inc index))
      name)))

(defn is-loaded [name]
  (or (when (not (nil? @current-template))
        (= (:name @current-template) name)) false))

(defn add-empty-template []
  (let [name (next-default-name DEFAULT_NAME (count @project-templates))
        template (ProjectTemplate. name
                                   DEFAULT_DESCRIPTION
                                   1
                                   false
                                   15
                                   #{}
                                   true
                                   #{}
                                   false)]
    (swap! project-templates conj template)))

(defn- silent-update [index template]
  (let [deref-templates @project-templates]
    (destroy-cell! project-templates)
    (->> (assoc-in deref-templates [index] template)
         (set-cell! project-templates))))

(defn- update-property [property value]
  (when (not (nil? @current-template))
    (swap! current-template assoc-in [property] value)
    (silent-update @current-template-index  @current-template)
    ))

(defn- merge-current [template]
  (->> (merge template @current-template)
       (reset! current-template)))

(defn add-template [template]
  (let [existing (by-name (:name template))]
    (when (nil? existing)
       (swap! project-templates conj template))))

(defn get-template [name]
  (by-name name))

(defn current-template-value [name]
  (name @current-template))

(defn get-property
  ([property] (cell= (when (not (nil? current-template))
                   (property current-template))))
  ([name property]
     (cell= (let [ind (template-index-of name)]
              (get-in project-templates [ind property])))))

(defn load-template [name]
  (println (str "loading template: " name))
  (let [template (by-name name)
        ind (tempate-index-of name)]
    (when (not (nil? template))
      (reset! current-template template)
      (reset! current-template-index ind)
      (println (str "Loaded template " (.stringify js/JSON (clj->js @current-template)))))))

(defn save-template []
  (println "Saving template"))

(defn templates []
  @project-templates)


(defn init-templates []
  (when (empty? @project-templates)
    (add-empty-template)))
