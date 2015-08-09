(ns core.project.project-services
  (:require [core.project.project :as project]
            [core.entities.entity :as e]
            [core.project.settings :as s]))

(defn- assert-keyword [tokeyword]
  (if (keyword? tokeyword) tokeyword (keyword tokeyword)))

(defn- serialize-page [pageid]
  (let [page-data (->> @project/project
                       :pages
                       ((assert-keyword pageid)))
        canvas (:canvas page-data)]
    {
    ; :svg (.toSVG canvas)
     :id (assert-keyword pageid)
     :number (:number page-data)
     :data {
            :entities (doall (map #(hash-map :type (:type %),:uid (:uid %)) (vals @e/entities)))
            :canvas (js->clj (.toObject canvas  (clj->js ["refId"]))) ;; doesnt goes through castra endpoint !!! change it to toDatalessJSON when image server will be ready.
            }
     }
    ))

(defn serialized-data []
  )

(defn serialize []
  {
   :settings @s/settings
   :pages (doall (map #(serialize-page (:id %)) (vals (:pages @project/project))))
  })

(defn cleanup []
  (project/cleanup-project-data [])
  ;(reset! e/entities {})
  )

(defn deserialize [data]
  (cleanup)
  )
