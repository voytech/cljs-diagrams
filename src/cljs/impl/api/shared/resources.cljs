(ns impl.api.shared.resources
  (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [tailrecursion.javelin :as j :refer [cell]]
            [impl.api.public.auth :as a])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc result {})
(defc resources-response [])
(defc resources {:background []
                 :clipart []
                 :photo []})

(defc loading [])

(defn- append-resource [resource]
  (let [cat-kw (keyword (:category resource))]
     (swap! resources assoc cat-kw (conj (or (-> @resources cat-kw) []) resource))))

(defc error {})

(cell= (doseq [resource resources-response]
         (append-resource resource)))

(cell= (if (:filename result)
         (append-resource result)
         (println "Hmmmmmmmm...! No filename returned !?")))

(cell= (when-let [status (:status error)]
         (println (str "status: " status))
         (if (= status :unauthenticated) (a/reset-login-state))))

(def put-resource (mkremote 'core.services.shared.resources-service/put-resource
                              result
                              error
                              loading
                              ["/app/shared"]))

(def get-resources (mkremote 'core.services.shared.resources-service/get-resources
                             resources-response
                             error
                             loading
                             ["/app/shared"]))

(defn resource-server-path [resource]
  (str (:path resource) "/" (:filename resource)))

(defn get-resource [name type]
  (let [tres (type @resources)]
    (first (filter #(= name (:filename %)) tres))))
