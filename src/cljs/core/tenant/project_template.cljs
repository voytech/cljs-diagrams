(ns core.tenant.project-template
  (:require [tailrecursion.javelin :refer [cell ]]
            [core.actions :as actions]))

(def ^:private DEFAULT_NAME "Enter name")
(def ^:private DEFAULT_DESCRIPTION "Enter description")

(def project-templates (cell {}))

(defrecord ProjectTemplate [
                            name
                            description
                            page-count
                            fixed-count?
                            max-page-count
                            page-formats
                            custom-format?
                            printing-surfaces
                            client-requests?])

(defn- next-default-name [name index]
  (let [found-name (get @project-templates name)]
    (if (not (nil? found-name))
      (recur (str DEFAULT_NAME " " index) (inc index))
      name)))

(defn add-empty-template []
  (let [template (ProjectTemplate. DEFAULT_NAME
                                   DEFAULT_DESCRIPTION
                                   1
                                   false
                                   15
                                   {}
                                   true
                                   {}
                                   false)]
    (swap! project-templates assoc-in [(next-default-name (:name template)
                                                          (count @project-templates))] template)))

(defn update-property [name property value]
  (let [existing (get @project-templates name)]
    (when (not (nil? existing))
      (dosync (swap! project-templates dissoc name)
              (let [merged (merge existing {property value})]
                  (swap! project-templates assoc-in [(:name merged)] merged))))))

(defn update-template [template]
  (let [existing (get @project-templates (:name template))]
    (when (not (nil? existing))
      (dosync (swap! project-templates dissoc (:name template))
              (->> (merge existing template)
                   (swap! project-templates assoc-in [(:name template)]))))))

(defn add-template [template]
  (let [existing (get @project-templates (:name template))]
    (if (not (nil? existing))
       (update-template template)
       (swap! project-templates assoc-in [(:name template)] existing))))

(defn get-template [name]
  (get @project-templates name))

(defn get-property [name property]
  (cell= get-in project-templates [name property])) ;; TODO: wonder if it will work. First level is map, second is record.

(defn templates []
  @project-templates)
