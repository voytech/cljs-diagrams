(ns core.services.public.api
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]
            [cemerick.friend :refer [authenticated *identity*]]))

(defrpc login [{{:keys [username password]} :authentication}]
  (println (str "User: " username ", Password " password))
  {:identity username})

(defrpc register [user passwd email]
  (authenticated {:message "register requires authentication"}
                 (println (str *identity*))
                 (println (str "User: " user ", Ema" email ", Password " passwd))))

(defrpc logout []
  (println "Logged out!"))
