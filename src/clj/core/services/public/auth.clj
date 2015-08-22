(ns core.services.public.auth
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]
            [cemerick.friend :refer [authenticated *identity*]]))

(defrpc register [user passwd email]
  (println (str *identity*))
  (println (str "User: " user ", Ema" email ", Password " passwd)))

(defrpc logout []
  (println "Logged out!"))
