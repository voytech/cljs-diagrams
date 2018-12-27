(ns impl.extensions.resolvers.default
 (:require [core.entities :as e]
           [impl.components :as c]
           [impl.features.default :as f]
           [clojure.spec.alpha :as spec]
           [core.eventbus :as bus]
           [extensions.data-resolvers :as r])
 (:require-macros [extensions.macros :refer [defresolver]]))

(defresolver set-title
             f/is-shape-entity
             (spec/keys :req-un [::title])
             (fn [app-state entity data]
               (e/assert-component c/title app-state entity "title" {:text (:title data)})
               (bus/fire app-state "uncommited.render")))
