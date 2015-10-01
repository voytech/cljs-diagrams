(ns impl.services.test-helpers
  (:require [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [core.db.schemap :refer [persist-schema]]
            [datomic.api :as d]
            [ring.mock.request :as mock]))

(def abspath "/home/voytech/programming/github/photo-collage/resources")

(defn reload-db [name url]
  (d/delete-database url)
  (let [f (future (Thread/sleep 70000)
                  (when-let [res (d/create-database url)]
                    (println (str "create-db " res))
                    (when-let [connection (d/connect url)]
                      (persist-schema name url))))]
    @f))

(defn mock-castra [path q-func payload]
  (-> (mock/request :post path)
      (mock/header "X-Castra-Csrf" "true")
      (mock/header "X-Castra-Tunnel" "cljson")
      (mock/header "Accept" "application/json")
      (mock/body (clj->cljson [q-func payload]))))

(defn mock-login [path username password]
  (-> (mock/request :post path)
      (mock/header "Authentication" (clj->cljson [username password]))
      (mock/header "Accept" "application/json")))

(defn parse-resp [resp]
  {:body (-> resp
             :body
             cljson->clj)
   :status (:status resp)})

(defn response-session [response]
  (-> response
      :headers
      (get "Set-Cookie")
      (first)))

(defn session-aware-request [username password request]
  (let [response (mock-login "/app/login" username password)
        session (response-session response)
        ensured (mock/header request "cookie" session)]))
