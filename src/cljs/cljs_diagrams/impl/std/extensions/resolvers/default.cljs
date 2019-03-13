(ns cljs-diagrams.impl.std.extensions.resolvers.default
 (:require [cljs-diagrams.core.entities :as e]
           [cljs-diagrams.core.components :as d]
           [cljs-diagrams.impl.std.components :as c]
           [cljs-diagrams.impl.std.behaviours.manhattan :as m]
           [cljs-diagrams.impl.layouts.flow :as fl]
           [cljs-diagrams.core.behaviour-api :as api]
           [cljs-diagrams.impl.std.behaviours.behaviour-api :as stdapi]
           [cljs-diagrams.impl.std.features.default :as f]
           [clojure.spec.alpha :as spec]
           [clojure.string :as str]
           [cljs-diagrams.core.rendering :as rendering]
           [cljs-diagrams.core.layouts :as l]
           [cljs-diagrams.extensions.data-resolvers :as r])
 (:require-macros [cljs-diagrams.extensions.macros :refer [defresolver]]))

(defn initialize [app-state]
  (r/register app-state
              ::set-title
              f/is-shape-entity
              (spec/keys :req-un [::title])
              (fn [app-state entity data]
                (e/assert-component c/title app-state entity "title" {:text (:title data)})
                (rendering/render-changes app-state)))

  (r/register app-state
              ::write-notes
              f/is-shape-entity
              (spec/keys :req-un [::notes])
              (fn [app-state entity data]
                (let [bbox (:bbox entity)
                      word-space 5
                      right-edge (+ (:width bbox) (:left bbox))
                      left (volatile! (:left bbox))
                      top  (volatile! (:top bbox))]
                  (doseq [[idx word] (map-indexed (fn [idx itm] [idx itm]) (str/split (:notes data) #"\s+"))]
                    (let [component (e/assert-component c/text app-state entity
                                                        {:name (str "note-wd-" idx)
                                                         :model {:text word :left @left :top @top}
                                                         :layout-attributes (fl/flow-layout-attributes "notes" idx 5)})]
                      (rendering/render-changes app-state)))
                  (l/do-layouts entity)
                  (rendering/render-entity app-state entity))))

  (r/register app-state
              ::make-association
              f/is-association-entity
              (spec/keys :req-un [::x1 ::y1 ::x2 ::y2])
              (fn [app-state entity data]
                (m/set-relation-endpoints app-state entity {:x (+ (:x1 data) (:left data))
                                                            :y (+ (:y1 data) (:top data))}
                                                           {:x (+ (:left data) (:x2 data))
                                                            :y (+ (:top data) (:y2 data))})
                (let [startpoint (first (e/get-entity-component entity ::c/startpoint))]
                  (api/collides? app-state
                                 startpoint
                                 f/is-shape-entity
                                 (fn [src trg]
                                    (e/connect-entities app-state (:entity trg) (:entity src) :start)
                                    (stdapi/toggle-controls (:entity trg) false)
                                    (m/refresh-manhattan-layout app-state entity))
                                 (fn [src])))
                (rendering/render-changes app-state))))
