(ns core.tools.tools-impl
  (:require [core.tools.tool :as tool]
            [utils.dom.dom-utils :refer [by-id]]
            [core.entities.entities-impl :as entities]
            [core.canvas-interface :as canvas]))


(defn photo-tool [name desc icon]
  (tool/create-tool name desc "photo" icon (fn [src ctx]
                                             (-> (entities/create-image-entity
                                                  (by-id (:name src))
                                                   ctx)
                                                 (canvas/add-entity)))))

(defn slot-tool []

)

(defn text-tool []

)
