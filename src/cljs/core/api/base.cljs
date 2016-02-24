(ns core.api.base
  (:require [tailrecursion.javelin :as j :refer [cell]])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

;;create page-info general api purpose object.
(defn paging-info [pagenr pagesize]
  {:nr pagenr, :size pagesize})

(defc loading [])
(defc error {})
(defc authentication {})

(defn- reset-login-state []
  (swap! authentication dissoc :identity))

(cell=
  (when-let [status (:status error)]
    (if (= status :unauthenticated) (reset-login-state))))
