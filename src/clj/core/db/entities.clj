(ns core.db.entities
  (:require [datomic.api :as d]
            [clojure.walk :refer [prewalk]]))

;; (defschema 'login-db
;;            {:mapping-inference true
;;             :auto-persist-schema true
;;             :db-url (mem-db-url)}
;;     (defentity 'user.login
;;       (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true} custom-fwd-mapping {} custom-rev-mapping {})
;;       (property name :password  type :db.type/string                            mapping-opts {:required true})
;;       (property name :roles     type :db.type/ref                               mapping-opts {:required true})
;;       (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref (fn [v] [:user.login/username v])}))
;;     (defentity 'tenant.login
;;       (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
;;       (property name :password      type :db.type/string                            mapping-opts {:required true})
;;       (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
;;       (property name :users         type :db.type/ref                               mapping-opts {:ref-type 'user.login})
;;       (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required false})))

;; Features :
;; 1. Perform mapping from service entities into database entities.
;; 2. Perform mapping back from database entity into service entities.
;; 3. Create datomic attribute schema from this entity schema.
;; 4. Automatically convert specific properties into lookup refs to establish relation.
;; 5. Persist nested component entities - automatically resolved from service entity

(declare map-entity
         var-by-symbol
         get-var
         reverse-mapping?)

(def DEFAULT_PARTITION :db.part/user)

(def ENTITY_TYPE_ATTRIB
  {:db/id #db/id[:db.part/db]
   :db/ident :entity/type
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc "Entity level shema attribute - name of entity"
   :db.install/_attribute :db.part/db})

(def ^:dynamic entities-frequencies {})
(def ^:dynamic schema {})

(defn get-var [symb]
  (->> symb
       name
       (str "core.db.entities/")
       symbol
       resolve
       var-get))

(defn get-schema [name]
  (get-in schema [(keyword name)]))

(defn current-schema-name []
  (get-var 'curr-schema))

(defn current-schema []
  (get-schema (get-var 'curr-schema)))

(defn make-temp-id []
  (let [partition (or (-> (current-schema) :mapping-opts :db-partition)
                      DEFAULT_PARTITION)]
    (d/tempid partition)))

(defn- initialize-database []
  (let [connection-string (-> (current-schema) :mapping-opts :db-url)]
    (d/create-database connection-string)))

(defn mapping-into-ns [mapping-symbol]
  (symbol (str "core.db.entities/" (name mapping-symbol))))

(defn db-mapping-type [mapping-type]
  (keyword (name mapping-type)))

(defn- mapping-enum [entity-name]
  [{:db/id (d/tempid :db.part/user),
    :db/ident (db-mapping-type entity-name)}])

(defn var-by-symbol [symbol]
  (-> symbol resolve var-get))

(defn inject-def [ns var-name value]
  (intern ns var-name value))

(defn make-var [symbol val]
  (intern 'core.db.entities symbol val))

(defn del-var [symb]
  (ns-unmap 'core.db.entities symb))

(defn get-frequencies []
  (var-by-symbol 'core.db.entities/entities-frequencies))

(defn- create-db-property [property-def]
  (-> {:db/id (d/tempid :db.part/db)
       :db/ident (:to-property property-def)
       :db/valueType (:type property-def)
       :db.install/_attribute :db.part/db}
      (merge (if-let [uniq  (:unique property-def)] {:db/unique uniq} {}))
      (merge (if-let [cmpnt (:component? property-def)] {:db/isComponent cmpnt} {}))
      (merge (if-let [card  (:cardinality property-def)] {:db/cardinality card} {:db/cardinality :db.cardinality/one}))))

(defn- append-schema [next-db-property]
  (alter-var-root #'schema (fn [o] (assoc-in schema [(keyword (current-schema-name)) :data] (conj (or (:data (current-schema)) []) next-db-property)))))

(defn persist-schema
  ([name url]
   (when-let [schema-data (:data (get-schema name))]
     (d/transact (d/connect (or url (-> (get-schema name) :mapping-opts :db-url))) schema-data)))
  ([name] (persist-schema name nil)))

(defn- do-check [key val]
  (when-not (and (symbol? key)
                 (contains? #{'name 'type 'cardinality 'mapping-opts 'index 'unique 'component?} key))
    (throw (IllegalArgumentException. (str "Wrong description of rule (" first "," val ")")))))

(defn- decode-args [args]
  (let [partitioned (partition 2 args)]
    (->> (doall (mapv (fn [p] (do-check (first p) (last p))
                        {(keyword (first p)), (last p)}) partitioned))
         (apply merge))))

(defmacro property [& args]
  (let [decoded (decode-args args)
        prop-name (:name decoded)
        entity-name (name (get-var 'curr-entity-name))
        to-property (keyword (str entity-name "/" (name (or (:to-property decoded) prop-name))))
        property-def (assoc decoded :to-property to-property)]
    (if (not (get-var 'flag))
      (do (append-schema (create-db-property property-def))
          {(:name decoded)
           (dissoc (assoc decoded :to-property to-property) :name)})
      {to-property {:to-property prop-name}})))

(defn concat-into [& items]
  (into []  (-> (apply concat items)
                distinct)))

(defmacro defentity [entity-name & rules]
  (d/transact (d/connect (-> (current-schema) :mapping-opts :db-url)) (mapping-enum  (eval entity-name)))
  (make-var 'curr-entity-name (eval entity-name))
  (let [entity-var (var-get (intern 'core.db.entities (eval entity-name)
                                    {:type (eval entity-name)
                                     :mapping (do (make-var 'flag false)
                                                  (apply merge (mapv #(eval %) rules)))
                                     :rev-mapping (do (make-var 'flag true)
                                                      (apply merge (mapv #(eval %) rules)))}))]
    (when (-> (current-schema) :mapping-opts :mapping-inference)
      (let [prop-map (apply merge (mapv (fn [k] {k [(:type entity-var)]}) (-> entity-var :mapping (keys))))]
        (alter-var-root #'entities-frequencies (fn [o] (merge-with concat-into entities-frequencies prop-map))))))
  (del-var 'curr-entity-name)
  (del-var 'flag)
  identity)

(defmacro defschema [n opts & defentities]
  (let [options (eval opts)
        name (name (eval n))] ;; canonical representation should be string. No matter if on input there is string symbol or keyword it would be string here.
    (alter-var-root #'schema (fn [o] {(keyword name) {:mapping-opts options}}))
    (make-var 'curr-schema name)
    (initialize-database)
    (d/transact (d/connect (-> (get-schema name) :mapping-opts :db-url)) [ENTITY_TYPE_ATTRIB])
    (eval defentities)
    (when (-> (get-schema name) :mapping-opts :auto-persist-schema)
      (persist-schema name)
      identity)))

(defn mapping-by-symbol [symbol]
  (-> symbol resolve var-get))

(defn- entity-less? [clj]
  (if (vector? clj) (entity-less? (first clj))
      (not (map? clj))))

(defn reverse-mapping? [entity]
  (if (isa? (type entity) clojure.lang.PersistentVector)
    (let [entry (first entity)] (reverse-mapping? entry))
    (:entity/type entity)))

(defn find-mapping [service-entity]
  (when-not (entity-less? service-entity)
    (if-let [entity-type (reverse-mapping? service-entity)]
      (->> (name entity-type) (str "core.db.entities/") symbol (var-by-symbol))
      (let [freqs (->> (mapv #(get entities-frequencies %) (keys service-entity))
                       (apply concat)
                       (frequencies))
            freqs-vec (mapv (fn [k] {:k k, :v (get freqs k)}) (keys freqs))
            max-val (apply max (mapv #(get freqs %) (keys freqs)))
            max-entries (filter #(= max-val (:v %)) freqs-vec)]
        (when (< 1 (count max-entries)) (throw (ex-info "Cannot determine mapping. At least two mappings with same frequency" {:entries max-entries})))
        (->> (first max-entries)
             :k
             name
             (str "core.db.entities/")
             symbol
             (var-by-symbol))))))


(defmulti apply-mapping-opts :opt-key)

(defmethod apply-mapping-opts :lookup-ref [{:keys [type func opt-key opt-value source target-property property-value]}]
  (make-var 'do-mapping? false)
  (swap! source assoc target-property (opt-value property-value)))

(defmethod apply-mapping-opts :ref-type [{:keys [type func opt-key opt-value source target-property property-value]}]
  (make-var 'do-mapping? false)
  (let [old (get-var 'do-mapping?)]
    (swap! source assoc target-property (func property-value
                                        (var-by-symbol (-> opt-value
                                                           mapping-into-ns))))
    (make-var 'do-mapping? old)))

(defmethod apply-mapping-opts :default [{:keys [type func opt-key opt-value source target-property property-value]}]
  (make-var 'do-mapping? false)
  (swap! source assoc target-property property-value))

(defn- map-property [mapping-func type from-property to-property source mapping-opts]
  (let [property-value (from-property @source)]
    (make-var 'do-mapping? true)
    (doseq [mapping-opt (keys mapping-opts)]
      (apply-mapping-opts {:type type
                           :func mapping-func
                           :opt-key mapping-opt
                           :opt-value (mapping-opt mapping-opts)
                           :source source
                           :target-property to-property
                           :property-value property-value}))
    (when (= true (get-var 'do-mapping?))
      (if (or (map? property-value)
              (vector? property-value))
        (swap! source assoc to-property (mapping-func property-value))
        (swap! source assoc to-property property-value)))
    (del-var 'do-mapping?))
  (swap! source dissoc from-property))

(defn- has? [property mapping]
  (when (not (contains? mapping property))
    (throw (ex-info "Mapping error. Source property doesn't contain corresponding mapping rule!"
                    {:property property
                     :mapping mapping}))))

(defn- delete-db-meta [source]
  (swap! source dissoc :db/id :entity/type))

(defn- add-db-meta [source-atom mapping]
  (swap! source-atom assoc :db/id (make-temp-id))
  (swap! source-atom assoc :entity/type (db-mapping-type (:type mapping))))

(defn- do-mapping [source-atom entity-type mapping-rules mapping-func]
  (let [source-props (keys @source-atom)]
    (doall (map #(do (has? % mapping-rules)
                     (map-property mapping-func
                                   entity-type
                                   %
                                   (:to-property (% mapping-rules))
                                   source-atom
                                   (:mapping-opts (% mapping-rules)))) source-props))))

(defmulti clj->db (fn ([source mapping] (type source))
                      ([source] (type source))))

(defmethod clj->db (type {})
  ([source mapping]
   (if mapping
     (let [mapping-rules (:mapping mapping)
           temp-source (atom source)]
       (do-mapping temp-source (:type mapping) mapping-rules clj->db)
       (add-db-meta temp-source mapping)
       @temp-source)
     source))
  ([source]
   (clj->db source (find-mapping source))))

(defmethod clj->db (type [])
  ([source mapping]
   (mapv #(clj->db % mapping) source))
  ([source]
   (clj->db source (find-mapping (first source)))))

(defmethod clj->db :default
  ([source mapping] source)
  ([source] source))

(defmulti db->clj (fn ([source mapping] (type source))
                      ([source] (type source))))

(defmethod db->clj (type {})
  ([source mapping]
   (if mapping
     (let [mapping-rules (:rev-mapping mapping)
           temp-source (atom source)]
       (delete-db-meta temp-source)
       (do-mapping temp-source (:type mapping) mapping-rules db->clj)
       @temp-source)
     source))
  ([source]
   (db->clj source (find-mapping source))))

;;TODO: Handle mapping of collection of non entities. e.g. collection of vectors.
(defmethod db->clj (type [])
  ([source mapping]
   (mapv #(db->clj % mapping) source))
  ([source]
   (db->clj source (find-mapping (first source)))))

(defmethod db->clj :default
  ([source mapping] source)
  ([source] source))
