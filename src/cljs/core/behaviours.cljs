(ns core.behaviours
  (:require [core.entities :as e]
            [core.events :as ev]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as bus]))

(defonce behaviours (volatile! {}))

(defonce active-behaviours (volatile! {}))

(defrecord Behaviour [name
                      display-name
                      type
                      validator
                      action
                      handler])
;Validator needs to return targets (keyword of component types for which behaviour can be registered)

(defn add-behaviour [name display-name type validator action handler]
  (vswap! behaviours assoc name (Behaviour. name display-name type validator action handler)))

(defn entity-behaviours [entity-type])

(defn component-behaviours [component-type])

(defn validate [behaviour components]
  (let [behaviour_ (cond
                    (string? behaviour) (get @behaviours behaviour)
                    (record? behaviour) (get @behaviours (:name behaviour)))]
    ((:validator behaviour_) components)))

(defn enable [entity component-type behaviour])

(defn disable [entity component-type behaviour])

(defn- make-key [entity-type-or-id attribute-name component-type behaviour-type]
  (cond-> #{}
          (not (nil? entity-type-or-id)) (conj entity-type-or-id)
          (not (nil? attribute-name)) (conj attribute-name)
          (not (nil? component-type)) (conj component-type)
          (not (nil? behaviour-type)) (conj behaviour-type)))

(defn get-active-behaviour [entity-type-or-id attribute-name component-type behaviour-type]
  (let [key (make-key entity-type-or-id attribute-name component-type behaviour-type)]
    (get @active-behaviours key)))

(defn- is-active [entity-type-or-id attribute-name component-type behaviour-type]
  (not (nil? (get-active-behaviour entity-type-or-id attribute-name component-type behaviour-type))))

(defn- set-active-behaviour [entity-type-or-id attribute-name component-type behaviour-type behaviour-name]
  (let [key (make-key entity-type-or-id attribute-name component-type behaviour-type)]
    (vswap! active-behaviours assoc key behaviour-name)))

(defn autowire
  ([entity-type components-inputs]
   (doseq [behaviour (vals @behaviours)]
      (when-let [targets (validate behaviour components-inputs)]
         (let [inputs  (into {} (map (fn [e] {(:type e) e}) components-inputs))
               valid-inputs (vals (select-keys inputs targets))
               kv (map (fn [e] {:k (make-key entity-type nil (:type e) (:type behaviour)) :v e}) valid-inputs)
               inactive-kv (filter #(nil? (get @active-behaviours (:k %))) kv)]
            (bus/on (mapv #(ev/event-name entity-type nil (-> % :v :type) (:action behaviour)) inactive-kv) (:handler behaviour))
            (doseq [k inactive-kv] (set-active-behaviour entity-type nil (-> k :v :type) (:type behaviour) (:name behaviour)))))))
  ([entity]
   (let [components (:components entity)
         type (:type entity)]
      (autowire type components))))

(defn generic-validator [_definitions]
  (fn [components]
    (let [types (set (map :type components))]
      (first (filter #(not (nil? %)) (map (fn [e] (when ((:func e) (:tmpl e) types) (:result e))) _definitions))))))

(defonce hooks (atom {}))

(defn to-the-center-of [line x y shape]
  (d/set-data line {x (+ (d/get-left shape) (/ (d/get-width shape) 2))
                    y (+ (d/get-top shape) (/ (d/get-height shape) 2))}))

(defn all [ & handlers]
  (fn [e]
    (doseq [handler handlers]
      (handler e))))

(defn event-wrap
  ([f]
   (fn [e]
     (let [entity (:entity e)]
       (f entity))))
  ([f & args]
   (fn [e]
     (let [entity (:entity e)
           drawable-name (:drawabe e)]
       (apply f entity drawable-name args)))))

(defn highlight [bln options]
 (fn [e]
   (d/set-data (:drawable e) {:border-color (if bln (:highlight-color options)
                                                    (:normal-color options))
                              :border-width (if bln (:highlight-width options)
                                                    (:normal-width options))})))

(defn show [entity component-name show]
 (let [component (e/get-entity-component entity component-name)]
   (d/setp (:drawable component) :visible show)))


(defn intersects-endpoints? [yes]
 (fn [e]
   (let [entity           (:entity e)
         component        (:component e)
         drawable         (:drawable e)]
     (when (contains? #{"end" "start"} (:name component))
       (doseq [drwlb (vals @d/drawables)]
          (when (and (not= drwlb drawable) (= :endpoint (:type (e/lookup drwlb :component))))
            (let [trg-ent  (e/lookup drwlb :entity)
                  trg-comp (e/lookup drwlb :component)]
              (when (d/intersects? drawable drwlb)
                (yes {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})))))))))

(defn intersects? [target-name yes no]
 (fn [e]
   (let [entity           (:entity e)
         component        (:component e)
         drawable         (:drawable e)]
     (when (contains? #{"end" "start"} (:name component))
       (doseq [drwlb (vals @d/drawables)]
          (when (= target-name (:name (e/lookup drwlb :component)))
            (let [trg-ent  (e/lookup drwlb :entity)
                  trg-comp (e/lookup drwlb :component)]
               (if (d/intersects? drawable drwlb)
                 (yes {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})
                 (no  {:drawable drawable :component component :entity entity} {:drawable drwlb :component trg-comp :entity trg-ent})))))))))

(defn calculate-offset [component left top]
 {:left (- left (d/getp (:drawable component) :left))
  :top  (- top  (d/getp (:drawable component) :top))})

(defn calculate-effective-offset [entity component-name left top coord-mode]
 (if (= :offset coord-mode)
   {:left left :top top}
   (let [component (e/get-entity-component entity component-name)]
     (calculate-offset component left top))))

(defn effective-position
 ([component get-x get-y x y coord-mode]
  (let [effective-x (if (= :offset coord-mode) (+ (get-x (:drawable component)) x) x)
        effective-y  (if (= :offset coord-mode) (+ (get-y (:drawable component)) y) y)]
    {:x effective-x :y effective-y}))
 ([component x y coord-mode]
  (effective-position component #(d/getp % :left) #(d/getp % :top) x y coord-mode)))

(defn apply-effective-position
 ([component set-x get-x set-y get-y x y coord-mode]
  (let [epos (effective-position component get-x get-y x y coord-mode)]
    (set-x (:drawable component) (:x epos))
    (set-y (:drawable component) (:y epos))))
 ([comopnent x y coord-mode]
  (apply-effective-position comopnent
                            #(d/setp %1 :left %2)
                            #(d/getp % :left)
                            #(d/setp %1 :top %2)
                            #(d/getp % :top)
                            x
                            y
                            coord-mode)))

(defn- position-attributes-components [attributes offset-left offset-top]
  (doseq [src (flatten (mapv #(e/components %) attributes))]
    (d/set-data (:drawable src) {:left (+ (d/getp (:drawable src) :left) offset-left)
                                 :top  (+ (d/getp (:drawable src) :top) offset-top)})))

(defn default-position-entity-component [entity component-name left top coord-mode]
  (let [component (e/get-entity-component entity component-name)]
    (apply-effective-position component left top coord-mode)))

(defn default-position-entity [entity ref-component-name left top coord-mode]
 (let [effective-offset (calculate-effective-offset entity ref-component-name left top coord-mode)]
   (doseq [component (e/components entity)]
     (let [effective-left  (+ (d/getp (:drawable component) :left) (:left effective-offset))
           effective-top   (+ (d/getp (:drawable component) :top) (:top effective-offset))]
       (if (= ref-component-name (:name component))
         (default-position-entity-component entity (:name component) left top :offset)
         (default-position-entity-component entity (:name component) effective-left effective-top :absolute))))
   (position-attributes-components (:attributes entity) (:left effective-offset) (:top effective-offset))))

(defn default-position-related-entity [entity related-entity relation left top coord-mode]
  (when-let [hook (get-in @hooks [(:type entity) (:type related-entity)])]
    (hook entity related-entity relation left top coord-mode)))

(defn moving-entity []
 (fn [e]
   (let [entity (:entity e)
         event (:event e)
         component (:component e)
         movementX (:movement-x e)
         movementY (:movement-y e)]
     (default-position-entity entity
                              (:name component)
                              movementX
                              movementY
                              :offset)
     (doseq [relation (:relationships entity)]
       (let [related-entity (e/entity-by-id (:entity-id relation))]
          (default-position-related-entity   entity
                                             related-entity
                                             relation
                                             movementX
                                             movementY
                                             :offset))))))

(defn relations-validate [entity]
 (doseq [relation (:relationships entity)]
   (let [related-entity (e/entity-by-id (:entity-id relation))
         target-bbox (layouts/get-bbox related-entity)
         source-bbox (layouts/get-bbox entity)]
     (when (not (layouts/intersects? source-bbox target-bbox))
       (e/disconnect-entities entity related-entity)))))

(defn set-relation-movement-hook [sent-type rent-type hook]
  (swap! hooks assoc-in [sent-type rent-type] hook))
