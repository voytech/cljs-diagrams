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
  (tool/create-tool "Placeholder"
                    "Insert other elements to fit placeholder"
                    "placeholder"
                    icon
                    (fn [src ctx]
                        (-> (entities/create-slot-entity ctx)
                            (canvas/add-entity))))
)

(defn text-tool []
  (tool/create-tool "Write Text!"
                    "Drag and write a text here."
                    "text"
                    icon
                    (fn [src ctx]
                       (-> (entities/create-text-entity "Enter text..." ctx)
                           (canvas/add-entity))))
)
