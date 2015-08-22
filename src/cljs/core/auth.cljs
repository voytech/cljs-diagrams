(ns core.auth
  (:require [tailrecursion.castra  :as c :refer [async]]))

(defn login [username password]
  (async "/login" `[~username ~password]
         #() ;success handler
         #() ;error handler
         #() ;run always
         :ajax-impl nil))
