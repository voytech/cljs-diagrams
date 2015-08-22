(ns core.project.project-services
  (:require [core.project.project :as project]
            [core.entities.entity :as e]
            [core.project.settings :as s]
            [utils.dom.dom-utils :as dom]))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defn- serialize-page [page]
  (let [canvas (:canvas page)]
    {
     :svg (.toSVG canvas)
     :id (assert-keyword (:id page))
     :number (:number page)
     :data {
            :entities (doall (map #(hash-map :type (:type %),:uid (:uid %)) (vals @e/entities)))
            :canvas (js->clj (.toObject canvas  (clj->js ["refId"]))) ;; doesnt goes through castra endpoint !!! change it to toDatalessJSON when image server ready.
           }
    }))

(defn- load-page [page-json]
  (let [canvas-clj-data (-> page-json :data :canvas)
        canvas-js-data (-> canvas-clj-data clj->js)
        entities (-> page-json :data :entities)
        page-id (-> page-json :id name)]
    (->> page-id project/create-page)
    (dom/wait-on-element page-id
                         #(let [canvas (-> page-json :id project/proj-page-by-id :canvas)]
                              (.loadFromJSON canvas canvas-js-data
                                             (fn []
                                               (doseq [index (range 0 (.-length (.getObjects canvas)))] ;Does it include background ?
                                                 (let [src (.item canvas index)
                                                       ent (first (filter (fn [e] (= (:uid e) (.-refId src))) entities))]
                                                   (e/create-entity-for-type (:type ent) src)
                                                   ))
                                                 )
                                               )))))

(defn cleanup-project-data []
  (project/cleanup-project-data))

(defn serialize-project-data []
  {
   :settings @s/settings
   :pages (doall (map #(serialize-page %) (vals (:pages @project/project))))
  })

(defn deserialize-project-data [data]
  (cleanup-project-data)
  (reset! s/settings (:settings data))
  (doall (map #(load-page %) (->> data :pages (sort-by :number))))
  )
