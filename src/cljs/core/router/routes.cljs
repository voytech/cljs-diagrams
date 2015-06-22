(ns core.router.routes
  (:require [core.router.router :as router]
            [ui.pages.main :refer [main]]
            [ui.pages.admin :refer [admin]]
))

(router/set-container-id "main-container" )
(router/def-route "#/main" (main))
(router/def-route "#/admin" (admin))
