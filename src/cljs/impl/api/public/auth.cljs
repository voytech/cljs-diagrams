(ns impl.api.public.auth
 (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
           [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
           [tailrecursion.javelin :as j :refer [cell]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc register-resp {})
(defc logout-resp {})
(defc login-resp {})
(defc error nil)
(defc loading [])

(def register  (mkremote 'core.services.public.auth/register
                          register-resp
                          error
                          loading
                          ["/app/public"]))

(def logout    (mkremote 'core.services.public.auth/logout
                          logout-resp
                          error
                          loading
                          ["/app/public"]))

;; (defn jq-ajax [async? url data headers done fail always]
;;   (.. js/jQuery
;;     (ajax (clj->js {"async"       async?
;;                     "contentType" "application/json"
;;                     "data"        data
;;                     "dataType"    "text"
;;                     "headers"     headers
;;                     "processData" false
;;                     "type"        "POST"
;;                     "url"         url}))
;;     (done (fn [_ _ x] (done (aget x "responseText"))))
;;     (fail (fn [x _ _] (fail (aget x "responseText"))))
;;     (always (fn [_ _] (always)))))


(defn login [username password]
  (jq-ajax false
           "/app/login"
           nil
           {"authentication" (clj->cljson [username password])
            "Accept"          "application/json"}
           #((do (println (str "resp " %)) (reset! login-resp (cljson->clj %))))
           #(reset! error (cljson->clj %))
           #()))
