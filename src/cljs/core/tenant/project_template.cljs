(ns core.tenant.project-template
  (:require [tailrecursion.javelin :refer [cell destroy-cell! set-cell!]]
            [core.project.settings :refer [settings!]]
            [core.project.project-services :refer [serialize-project-data
                                                   deserialize-project-data
                                                   cleanup-project-data]]
            [core.tenant.api.templates-api :as api]
            [utils.dom.dom-utils :as dom]
            )
  (:require-macros [tailrecursion.javelin :refer [cell= dosync]]))

(def ^:private DEFAULT_NAME "Enter name")
(def ^:private DEFAULT_DESCRIPTION "Enter description")

(def all-templates (cell []))
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
                            current-format
                            custom-format?
                            printing-surfaces
                            current-surface
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
  (let [found-name (first (filter #(= name (:name %)) @all-templates))]
    (if (not (nil? found-name))
      (recur (str DEFAULT_NAME " " index) (inc index))
      name)))

(defn is-loaded [name]
  (or (when (not (nil? @current-template))
        (= (:name @current-template) name)) false))

(defn add-empty-template []
  (let [name (next-default-name DEFAULT_NAME (count @all-templates))
        template (ProjectTemplate. name
                                   DEFAULT_DESCRIPTION
                                   1
                                   false
                                   15
                                   #{}
                                   nil
                                   true
                                   #{}
                                   nil
                                   false)]
    (println (str "Adding template: " (.stringify js/JSON (clj->js template))))
    (swap! all-templates conj template)))

(defn- silent-update [index template]
  (let [deref-templates @project-templates]
    (destroy-cell! project-templates)
    (->> (assoc-in deref-templates [index] template)
         (set-cell! project-templates))))

(defn- update-property [property value]
  (when (not (nil? @current-template))
    (swap! current-template assoc-in [property] value)
    (silent-update @current-template-index  @current-template)
   ; (swap! all-templates assoc-in)
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
        ind (template-index-of name)]
    (when (not (nil? template))
      (reset! current-template template)
      (reset! current-template-index ind)
      (println (str "Loaded template " (.stringify js/JSON (clj->js @current-template)))))))

(defn save-template []
  (->> {
        :project (serialize-project-data)
        :template-name (:name @current-template)
        ;:template @current-template
        }
       (api/save-template!)))

(defn templates []
  @project-templates)

(defn check-page [pagenr items-per-page]
  (let [start (* (- pagenr 1) items-per-page)]
    (< start (dec (count @all-templates)))))

(defn change-page [pagenr items-per-page]
   (when (check-page pagenr items-per-page)
     (let [start  (* (- pagenr 1) items-per-page)
           end  (+ start items-per-page)
           subv (subvec @all-templates start end)
           ]
       (println (str "change page : " pagenr ", " start ", " end))
       (reset! project-templates subv)
       subv)))

(defn init-templates []
  (when (empty? @project-templates)
    (dotimes [n 20] (add-empty-template)))

  (cell= (when (not (nil? current-template))
           (let [pcount (:page-count current-template)]
             (settings! pcount :pages :count))))
  (cell= (when (not (nil? current-template))
           (let [format (:current-format current-template)]
             (settings! format :page-format)))))
