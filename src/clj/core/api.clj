(ns core.api
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]))

(defrpc login [user passwd]
  (println (str "User: " user ", Password " passwd)))

(defrpc register [user passwd email]
  (println (str "User: " user ", Email " email ", Password " passwd)))

(defrpc logout []
  (println "Logged out!"))
