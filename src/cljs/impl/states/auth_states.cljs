(ns impl.states.auth-states
  (:require [impl.api.public.auth :as a]
            [core.router.router :as r]
            [tailrecursion.javelin :refer [cell]])
  (:require-macros [tailrecursion.javelin :refer [defc= cell= dosync]]))

;;Seems it will no longer be required.
