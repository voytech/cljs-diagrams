(ns impl.entities
 (:require [core.entities :as e]
           [impl.standard-attributes :as stdatr]
           [core.project :as p]
           [core.behaviours :as cb :refer [highlight show all event-wrap]]
           [core.options :as o]
           [core.entities-behaviours :as eb :refer [endpoint moving-entity relation-line
                                                    insert-breakpoint dissoc-breakpoint moving-endpoint
                                                    intersects? intersects-any? toggle-endpoints
                                                    position-endpoint position-startpoint relations-validate arrow]]
           [clojure.string :as str])
 (:require-macros [core.macros :refer [defentity with-drawables]]))

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
  (with-drawables data options
    (let [enriched-opts (merge options
                               eb/DEFAULT_SIZE_OPTS
                               eb/TRANSPARENT_FILL
                               eb/DEFAULT_STROKE
                               eb/RESTRICTED_BEHAVIOUR
                               eb/NO_DEFAULT_CONTROLS)
          conL    (vector (:left options) (+ (/ (:height eb/DEFAULT_SIZE_OPTS) 2) (:top options)))
          conR    (vector (+ (:left options) (:width eb/DEFAULT_SIZE_OPTS)) (+ (/ (:height eb/DEFAULT_SIZE_OPTS) 2) (:top options)))
          conT    (vector (+ (/ (:width eb/DEFAULT_SIZE_OPTS) 2) (:left options)) (:top options))
          conB    (vector (+ (/ (:width eb/DEFAULT_SIZE_OPTS) 2) (:left options)) (+ (:top options) (:height eb/DEFAULT_SIZE_OPTS)))]
      [{:name "connector-left"
        :type :endpoint
        :src (endpoint conL :moveable false :display "rect" :visibile false)}
       {:name "connector-right"
        :type :endpoint
        :src (endpoint conR :moveable false :display "rect" :visibile false)}
       {:name "connector-top"
        :type :endpoint
        :src (endpoint conT :moveable false :display "rect" :visibile false)}
       {:name "connector-bottom"
        :type :endpoint
        :src (endpoint conB :moveable false :display "rect" :visibile false)}
       {:name "body"
        :type :main
        :src (js/fabric.Rect. (clj->js enriched-opts))}]))
  (with-attributes [#(stdatr/name % "<Enter name here>")
                    #(stdatr/description % "<Enter descrition here>")
                    #(stdatr/state % :open)]))



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

  (with-drawables data options
    (let [enriched-opts (merge options eb/DEFAULT_SIZE_OPTS eb/DEFAULT_STROKE eb/RESTRICTED_BEHAVIOUR eb/NO_DEFAULT_CONTROLS)
          offset-x (:left options)
          offset-y (:top options)
          points-pairs (partition 2 data)
          points-pairs-offset (map #(vector (+ (first %) offset-x) (+ (last %) offset-y)) points-pairs)
          conS (first points-pairs-offset)
          conE (last points-pairs-offset)]
        [{:name "connector"
          :type :relation
          :src  (relation-line (first conS) (last conS) (first conE) (last conE) enriched-opts)
          :props {:start "start" :end "end"}}

         {:name "start"
          :type :startpoint
          :src  (endpoint conS :moveable true :display "circle" :visible true :opacity 1)
          :props {:start "connector" :penultimate true}}

         {:name "arrow"
          :type :decorator
          :src  (arrow data options)}

         {:name "end"
          :type :endpoint
          :src  (endpoint conE :moveable true :display "circle" :visible true :opacity 0)
          :props {:end "connector"}}])))
