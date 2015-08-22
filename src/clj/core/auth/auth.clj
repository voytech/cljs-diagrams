(ns core.auth.auth
  (:require [tailrecursion.castra.middleware :as c]
            [tailrecursion.castra  :as r]
            [ring.util.request :as rur]
            [cemerick.friend.workflows :as cfw]
            [cemerick.friend :as friend]
            [cognitect.transit   :as t])
  (:import  [java.io ByteArrayInputStream ByteArrayOutputStream])
  )


(def clj->json
  (atom #(let [out (ByteArrayOutputStream. 4096)]
           (t/write (t/writer out :json) %2)
           (.toString out))))

(def json->clj
  (atom #(-> (ByteArrayInputStream. (.getBytes %2)) (t/reader :json) t/read)))

(defn- castra-decode [req]
 (@json->clj req (rur/body-string req)))

(defn username-passwd-auth? [request]
  (let [[f & args] (castra-decode request)
        arg (last args)
        [_ [_ key] [_ [_ username-k] username-v  [_ password-k] password-v]] arg
        ]
    (println (str "castra-decode " key " username " username-v " password " password-v))
    (if (and (not (nil? key))
             (not (nil? username-k))
             (not (nil? username-v))
             (not (nil? password-k))
             (not (nil? password-v)))
      {:username username-v
       :password password-v}
      false))
    )

(def global-credential-fn
  (fn [{:keys [username password cemerick.friend/workflow]}]
      {:username "Wojciech"
       :roles [:TENANT :USER]
       }))

(def global-unauthorized-handler
 (fn [req] {:status 403 :body "Unauthorized (friend handler)"})
)

(def global-unauthenticated-handler
 (fn [req] {:status 401 :body "Unauthenticated (friend handler)"})
)

;; ONLY tests.
(defn username-password-authentication-workflow
"This workflow provides standard username password authentication for application
 Note that this workflow is out of the scope of castra RPC handler.
  "
[& {:keys [credential-fn realm] :as basic-config}]
  (fn [req]
    (println "castra workflow:")
     (cfw/make-auth {:username "wojtek"
                     :roles [:USER]}
                    {:cemerick.friend/workflow :castra
                     :cemerick.friend/redirect-on-auth? false})
    ;; (let [auth-data (username-passwd-auth? req)]
    ;;     (println (str "auth-data " auth-data))
    ;;     (if auth-data
    ;;       (let [user-record (-> auth-data (assoc :cemerick.friend/workflow :castra) credential-fn)]
    ;;         (if (not (nil? user-record))
    ;;           (cfw/make-auth user-record {:cemerick.friend/workflow :castra
    ;;                                       :cemerick.friend/redirect-on-auth false})
    ;;           {:status 401 :body "Unauthenticated."}))
    ;;       ))
    ))
