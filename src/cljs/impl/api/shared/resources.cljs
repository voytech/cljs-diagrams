(ns impl.api.shared.resources
  (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [tailrecursion.javelin :as j :refer [cell]]
            [utils.dom.dom-utils :as dom]
            [impl.api.public.auth :as a]
            [core.api.base :as auth])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc result {})
(defc resources-response [])
(defc resources {:background []
                 :clipart []
                 :photo []})


(defn- append-resource [resource]
  (let [cat-kw (keyword (:category resource))]
     (swap! resources assoc cat-kw (conj (or (-> @resources cat-kw) []) resource))))

(cell= (doseq [resource resources-response]
         (append-resource resource)))

(cell= (if (:filename result)
         (append-resource result)))


(def put-resource (mkremote 'core.services.shared.resources-service/put-resource
                              result
                              auth/error
                              auth/loading
                              ["/app/shared"]))

(def get-shared-resources (mkremote 'core.services.shared.resources-service/get-shared-resources
                             resources-response
                             auth/error
                             auth/loading
                             ["/app/shared"]))

(def all-shared-resources (mkremote 'core.services.shared.resources-service/all-shared-resources
                             resources-response
                             auth/error
                             auth/loading
                             ["/app/shared"]))

(def get-user-resources (mkremote 'core.services.shared.resources-service/get-user-resources
                             resources-response
                             auth/error
                             auth/loading
                             ["/app/shared"]))

(def all-user-resources (mkremote 'core.services.shared.resources-service/all-user-resources
                             resources-response
                             auth/error
                             auth/loading
                             ["/app/shared"]))

(defn resource-server-path [resource]
  (str (:path resource) "/" (:filename resource)))

(defn get-resource [name type]
  (let [tres (type @resources)]
    (first (filter #(= name (:filename %)) tres))))
