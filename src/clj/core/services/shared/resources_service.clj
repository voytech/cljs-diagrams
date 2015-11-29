(ns core.services.shared.resources-service
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema db->clj]]
            [impl.db.schema :refer :all]
            [cemerick.friend :as friend]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]
            [clojure.java.io :as cjo]
            [ring.util.codec :as b64]
            [conf :as cf]))

(defrpc make-category [dburl data]
  (binding [*database-url* dburl]
    (store-entity data)))

(defrpc all-categories []
  (binding [*database-url* (tenant-db-url)]
    (query-by-property :resource.category/name)))

(defn- resource-path [username meta]
  (if (nil? username)
    (str "SHARED/" (:category meta))
    (str username "/" (:category meta))))

(defn- create-resource-data [filename data]
  (let [abs-filename (str (:resource-path cf/configuration) filename)]
    (cjo/make-parents abs-filename)
    (with-open [out (clojure.java.io/output-stream (clojure.java.io/file abs-filename))]
      (.write out data))))

(defn- decode [data]
  (b64/base64-decode (last (clojure.string/split data #","))))

(defn put-resource-meta [meta]
  (let [ident (friend/current-authentication)
        username (:username ident)
        uid (:external-id ident)
        path (resource-path username meta)
        db (if (nil? uid) *shared-db* (tenant-db-url))]
    (binding [*database-url* db]
      (when-let [resource (-> (if (nil? uid) meta (assoc meta :owner uid))
                              (assoc :path path)
                              (dissoc :data))]
        (if-let [id (store-entity resource)]
          (assoc resource :id id)
          nil)))))

(defrpc put-resource [data]
  {:rpc/query [(load-entity [:resource.file/filename (:filename data)] (tenant-db-url))]}
  (when-let [meta (put-resource-meta data)]
    (create-resource-data (str (:path meta) "/" (:filename meta)) (decode (:data data)))))

(defn all-resources [dburl]
  (binding [*database-url* dburl]
    (query-by-property :resource.file/filename)))

(defn get-resources [dburl category]
  (binding [*database-url* dburl]
    (query-by-property :resource.file/category [:resource.category/name category])))

(defrpc all-shared-resources []
  (all-resources *shared-db*))

(defrpc get-shared-resources [category]
  (get-resources *shared-db*))

(defrpc all-user-resources []
  (all-resources (tenant-db-url)))

(defrpc get-user-resources [category]
  (get-resources (tenant-db-url)))

(defrpc get-resources-page [category {:keys [page-nr page-size] :as paging-opts}]
  )
