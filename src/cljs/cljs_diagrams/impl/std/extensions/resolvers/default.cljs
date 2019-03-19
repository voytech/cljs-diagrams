(ns cljs-diagrams.impl.std.extensions.resolvers.default
 (:require [cljs-diagrams.core.nodes :as e]
           [cljs-diagrams.core.shapes :as d]
           [cljs-diagrams.impl.std.shapes :as c]
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
              f/has-node-shape
              (spec/keys :req-un [::title])
              (fn [app-state node data]
                (e/assert-shape c/title app-state node "title" {:text (:title data)})
                (rendering/render-changes app-state)))

  (r/register app-state
              ::write-notes
              f/has-node-shape
              (spec/keys :req-un [::notes])
              (fn [app-state node data]
                (let [bbox (:bbox node)
                      word-space 5
                      right-edge (+ (:width bbox) (:left bbox))
                      left (volatile! (:left bbox))
                      top  (volatile! (:top bbox))]
                  (doseq [[idx word] (map-indexed (fn [idx itm] [idx itm]) (str/split (:notes data) #"\s+"))]
                    (let [shape (e/assert-shape c/text app-state node
                                                        {:name (str "note-wd-" idx)
                                                         :model {:text word :left @left :top @top}
                                                         :layout-attributes (fl/flow-layout-attributes "notes" idx 5)})]
                      (rendering/render-changes app-state)))
                  (l/do-layouts app-state node)
                  (rendering/render-node app-state node))))

  (r/register app-state
              ::make-association
              f/is-association-node
              (spec/keys :req-un [::x1 ::y1 ::x2 ::y2])
              (fn [app-state node data]
                (m/set-relation-endpoints app-state node {:x (+ (:x1 data) (:left data))
                                                          :y (+ (:y1 data) (:top data))}
                                                         {:x (+ (:left data) (:x2 data))
                                                          :y (+ (:top data) (:y2 data))})
                (let [startpoint (first (e/get-node-shape node ::c/startpoint))]
                  (api/collides? app-state
                                 startpoint
                                 f/has-node-shape
                                 (fn [src trg]
                                    (e/connect-nodes app-state (:node trg) (:node src) :start)
                                    (stdapi/toggle-controls (:node trg) false)
                                    (m/refresh-manhattan-layout app-state node))
                                 (fn [src])))
                (rendering/render-changes app-state))))
