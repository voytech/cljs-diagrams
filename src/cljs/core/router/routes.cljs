(ns core.router.routes
  (:require [core.router.router :as router]
            [ui.pages.main :as m]
            [ui.pages.admin :as a]
))

(router/set-container-id "main-container" )
(router/def-route "main" (m/main))
(router/def-route "admin" (a/admin))
