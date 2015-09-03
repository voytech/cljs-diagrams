(ns core.db.entities)

; Below mapping should be defined using concise macro defined api as follows:
; (defentity user-login
;   (from :username to :user/name     with {:required true})
;   (from :password to :user/password with {:required true})
;   (from :roles    to :user/roles    with {:required true})
;   (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))
; Above defentity is a macro and from is also macro which should be expanded.
; Entity will add datomic attribute :shared/type representing type of mapping, when pulling entities.
(def user-login {
                 :type :user-login,
                 :mapping {
                                :username {:key :user/name,
                                            :required true},
                                :password {:key :user/password,
                                            :required true},
                                :roles    {:key  :user/roles,
                                            :required true},
                                :tenant   {:key :user/tenant,
                                            :lookup-ref #([:user/name %])}}

                                :user/name     {:key :username},
                                :user/password {:key :password}
                 })


(defn- validate [property-1 direction property-2 with opts-map]
  (when (not (keyword? property-1))  (throw (IllegalArgumentException. "first arg must be keyword")))
  (when (not (symbol? direction))    (throw (IllegalArgumentException. "second argument must be symbol 'to'")))
  (when (not (keyword? property-2))  (throw (IllegalArgumentException. "third argument must be keyword")))
  (when (not (symbol? with))         (throw (IllegalArgumentException. "fourth argument must be symbol"))))

(defmacro from [property to attribute with opts-map]
     (validate property to attribute with opts-map)
    `{~property (merge {:key ~attribute} ~opts-map)})

(defn- property-to-entity [property entity-name]
  )

(defmacro defentity [entity-name opts & rules]
  `(do (create-ns 'mappings.runtime)
       (let [entity-var (intern 'mappings.runtime ~entity-name
                                {:type (keyword ~entity-name)
                                 :mapping (apply merge (map #(macroexpand-1 %) (list ~@rules))) ; I think map and macroexpand is not needed.
                                 }
                                )]
         (when (:auto-resolve ~opts)
           (let [prop-map (apply merge (map #({% [(:type entity-var)]}) (-> entity-var :mappings (keys))))]
             (swap! *entities-frequencies* (partial merge-with concat) prop-map)))
         entity-var
         )))

(defmacro pre [& defentities]
  `(do (create-ns 'mappings.runtime)
      (def ^:dynamic *entities-frequencies* nil)
      (binding [*entities-frequencies* (atom {}) ] ; may use no atom just a map and the update var root binding :)
        ~@defentities
        (intern 'mappings.runtime 'entities-frequencies @*entities-frequencies*))))

(defn find-mapping [service-entity]
  (require 'mappings.runtime)
  )

(defn map-entity
  ([source])
  ([source explicit-mapping]
   (postwalk #(when (keyword? %)
                (when-let [found (get (:mapping explicit-mapping) %)]
                       (:key found))) explicit-mapping)))
