(ns impl.entities
 (:require [core.entities :as e]
           [core.options :as defaults]
           [impl.standard-attributes :as stdatr]
           [impl.drawables :as d]
           [core.project :as p]
           [core.eventbus :as bus]
           [core.behaviours :as cb :refer [highlight show all event-wrap moving-entity intersects? intersects-any? relations-validate]]
           [core.options :as o]
           [impl.behaviours.standard :as eb :refer [insert-breakpoint dissoc-breakpoint moving-endpoint
                                                    toggle-endpoints position-endpoint position-startpoint]]
           [clojure.string :as str])
 (:require-macros [core.macros :refer [defentity with-components]]))

; After implementing EventBus -> implement plugins which will be functions of type pluing-name(obj-group,obj-type) -> void.
; This plugin will register specific event handling on bus which will allow e.g. implementing behavioural plugins based
; on originalEvents from DOM or Canvas frameworks. with-vehaviours can be changed then to accept just set of plugins.
(defentity rectangle-node
  (with-content-bounding-box {:left 15
                              :top  15
                              :width  175
                              :height 150})
  (with-behaviours
    {:endpoint {"mouse:over" (event-wrap show true)
                "mouse:out"  (event-wrap show false)}
     :main     {"object:moving" (moving-entity)
                "mouse:over"    (highlight true o/DEFAULT_HIGHLIGHT_OPTIONS)
                "mouse:out"     (highlight false o/DEFAULT_HIGHLIGHT_OPTIONS)}})
  (with-components data options
    (let [enriched-opts (merge options defaults/DEFAULT_SIZE_OPTS defaults/TRANSPARENT_FILL defaults/DEFAULT_STROKE)
          conL    (vector (:left options) (+ (/ (:height defaults/DEFAULT_SIZE_OPTS) 2) (:top options)))
          conR    (vector (+ (:left options) (:width defaults/DEFAULT_SIZE_OPTS)) (+ (/ (:height defaults/DEFAULT_SIZE_OPTS) 2) (:top options)))
          conT    (vector (+ (/ (:width defaults/DEFAULT_SIZE_OPTS) 2) (:left options)) (:top options))
          conB    (vector (+ (/ (:width defaults/DEFAULT_SIZE_OPTS) 2) (:left options)) (+ (:top options) (:height defaults/DEFAULT_SIZE_OPTS)))]
      [{:name "connector-left"
        :type :endpoint
        :drawable (d/endpoint conL :moveable false :display "rect" :visibile false)}
       {:name "connector-right"
        :type :endpoint
        :drawable (d/endpoint conR :moveable false :display "rect" :visibile false)}
       {:name "connector-top"
        :type :endpoint
        :drawable (d/endpoint conT :moveable false :display "rect" :visibile false)}
       {:name "connector-bottom"
        :type :endpoint
        :drawable (d/endpoint conB :moveable false :display "rect" :visibile false)}
       {:name "body"
        :type :main
        :drawable (d/rect enriched-opts)}]))
  (with-attributes [#(stdatr/name % "<Enter name here>")
                    #(stdatr/description % "<Enter descrition here>")
                    #(stdatr/state % :open)]))

(bus/on ["rectangle-node.main.mousedrag"] -999 (fn [e]
                                                  (let [event (:context @e)]
                                                    ((moving-entity) event))))

(bus/on ["rectangle-node.main.mousemove"] -999 (fn [e]
                                                  (let [event (:context @e)]
                                                    ((highlight true o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                                                    (toggle-endpoints (:entity event) true)
                                                    (bus/fire "rendering.finish"))))

(bus/on ["rectangle-node.main.mouseout"] -999 (fn [e]
                                                  (let [event (:context @e)]
                                                    ((highlight false o/DEFAULT_HIGHLIGHT_OPTIONS) event)
                                                    (toggle-endpoints (:entity event) false)                                                    
                                                    (bus/fire "rendering.finish"))))

(defentity relation
  (with-content-bounding-box {:left 15
                              :top  15
                              :width  180
                              :height 150})
  (with-behaviours
    {:relation   {"mouse:up" (insert-breakpoint)
                  "object:moving" (all (moving-entity)
                                       (event-wrap relations-validate))}
     :startpoint {"object:moving" (all (moving-endpoint)
                                       (intersects? "body" (fn [src trg] (toggle-endpoints (:entity trg) true))
                                                           (fn [src trg] (toggle-endpoints (:entity trg) false))))
                  "mouse:up"      (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) :entity-link "start" "start")
                                                                                                                                               (toggle-endpoints (:entity trg) false)
                                                                                                                                               (position-startpoint (:entity src) (.-left (:src trg)) (.-top (:src trg)))))
                                       (event-wrap relations-validate))
                  "mouse:over"    (highlight true o/DEFAULT_HIGHLIGHT_OPTIONS)
                  "mouse:out"     (highlight false o/DEFAULT_HIGHLIGHT_OPTIONS)}
     :endpoint   {"object:moving" (all (moving-endpoint)
                                       (intersects? "body" (fn [src trg] (toggle-endpoints (:entity trg) true))
                                                           (fn [src trg] (toggle-endpoints (:entity trg) false))))
                   "mouse:up"      (all (intersects-any? #{"connector-top" "connector-bottom" "connector-left" "connector-right"} (fn [src trg] (e/connect-entities (:entity src) (:entity trg) :entity-link "end" "end")
                                                                                                                                                (toggle-endpoints (:entity trg) false)
                                                                                                                                                (position-endpoint (:entity src) (.-left (:src trg)) (.-top (:src trg)))))
                                        (event-wrap relations-validate))
                   "mouse:over"    (highlight true o/DEFAULT_HIGHLIGHT_OPTIONS)
                   "mouse:out"     (highlight false o/DEFAULT_HIGHLIGHT_OPTIONS)}
     :breakpoint  {"mouse:over"    (highlight true o/DEFAULT_HIGHLIGHT_OPTIONS)
                   "mouse:out"     (highlight false o/DEFAULT_HIGHLIGHT_OPTIONS)
                   "mouse:up"      (dissoc-breakpoint)
                   "object:moving" (moving-endpoint)}})

  (with-components data options
    (let [enriched-opts options
          offset-x (:left options)
          offset-y (:top options)
          points-pairs (partition 2 data)
          points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
          conS (first points-pairs-offset)
          conE (last points-pairs-offset)]
        [{:name "connector"
          :type :relation
          :drawable  (d/relation-line (first conS) (last conS) (first conE) (last conE) enriched-opts)
          :props {:start "start" :end "end"}}

         {:name "start"
          :type :startpoint
          :drawable  (d/endpoint conS :moveable true :display "circle" :visible true :opacity 1)
          :props {:start "connector" :penultimate true}}

         {:name "arrow"
          :type :decorator
          :drawable  (d/arrow data options)}

         {:name "end"
          :type :endpoint
          :drawable  (d/endpoint conE :moveable true :display "circle" :visible true :opacity 0)
          :props {:end "connector"}}])))
