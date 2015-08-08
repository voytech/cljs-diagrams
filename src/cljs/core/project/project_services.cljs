(ns core.project.project-services
  (:require [core.project.project :as project]
            [core.entities.entity :as e]))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defn- serialize-page [pageid]
  (let [page-data (->> @project/project
                       :pages
                       ((assert-keyword pageid)))
        canvas (:canvas page-data)]
    {
     :svg (.toSVG canvas)
     :id (assert-keyword pageid)
     :number (:number page-data)
     :data {
            :entities (doall (map #(hash-map :type (:type %),:uid (:uid %)) (vals @e/entities)))
            :canvas (.toObject canvas (clj->js ["refId"]))}
     }
    ))

(defn serialized-data []
  )

(defn serialize []
  {:pages (doall (map #(serialize-page (:id %)) (vals (:pages @project/project))))})

(defn deserialize [data]
  )

(defn cleanup [])
