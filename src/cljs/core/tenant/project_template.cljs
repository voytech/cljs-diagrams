(ns core.tenant.project-template
  (:require [tailrecursion.javelin :refer [cell ]]
            [core.actions :as actions])
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]))

(def ^:private DEFAULT_NAME "Enter name")
(def ^:private DEFAULT_DESCRIPTION "Enter description")

;;TODO:Cells below will be loaded from castra backend.
;;project-templates changes only when adding new template/ when paging.
(def project-templates (cell []))
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

(defn- update-current-prop [property value]
  (when (not (nil? @current-template))
    (println (str "updating " property ":" value))
    (swap! current-template assoc-in [property] value)))

(defn- merge-current [template]
  (let [loaded (is-loaded (:name template))]
    (when (= true loaded)
      (->> (merge template @current-template)
           (reset! current-template)))))

(defn update-property [name property value]
  (let [existing (by-name name)
        loaded (is-loaded name)
        index (index-of @project-templates existing)]
    (if (= true loaded)
        (update-current-prop property value)
        (when (not (nil? existing))
            (let [merged (merge existing {property value})]
               (swap! project-templates assoc-in [index] merged))))))

(defn update-template [template]
  (let [existing (by-name (:name template))
        loaded (is-loaded name)
        index (index-of @project-templates existing)]
    (if (= true loaded)
      (merge-current template)
      (when (not (nil? existing))
          (->> (merge existing template)
                     (swap! project-templates assoc-in [index]))))))

(defn add-template [template]
  (let [existing (by-name (:name template))]
    (if (not (nil? existing))
       (update-template template)
       (swap! project-templates conj existing))))

(defn get-template [name]
  (by-name name))

(defn get-property [name property]
  (get-in @project-templates [(index-of @project-templates (by-name name)) property])
  ;; (cell= (if (= (:name current-template) name)
  ;;          (property current-template)
  ;;          (println (get-in project-templates [(index-of project-templates (by-name name)) property]))
  ;;          )
  ;;        )
  )

(defn load-template [name]
  (println (str "loading template: " name))
  (let [template (by-name name)]
    (when (not (nil? template))
      (reset! current-template template)
      (println (str "Loaded template " (.stringify js/JSON (clj->js @current-template)))))))

(defn templates []
  @project-templates)


(defn init-templates []
  (when (empty? @project-templates)
    (add-empty-template)))
