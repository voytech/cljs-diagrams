(ns core.db.schemap
  (:require [datomic.api :as d]
            [clojure.walk :refer [postwalk]]))

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
         persisted-entity-type)

(def DEFAULT_PARTITION :db.part/user)
(def ^:private THIS_NS "core.db.schemap")
(def ENTITY_TYPE_ATTRIB
  {:db/id (d/tempid :db.part/db)                           ;#db/id[:db.part/db]
   :db/ident :entity/type
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc "Entity level shema attribute - name of entity"
   :db.install/_attribute :db.part/db})

(def ^:dynamic entities-frequencies {})
(def ^:dynamic schema {})
(def ^:dynamic *db-url* "")

(defn get-var [symb]
  (->> symb
       name
       (str THIS_NS "/")
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
  (let [partition (or (-> (current-schema) :schema-opts :db-partition)
                      DEFAULT_PARTITION)]
    (d/tempid partition)))

(defn- initialize-database []
  (let [connection-string (-> (current-schema) :schema-opts :db-url)]
    (d/create-database connection-string)))

(defn mapping-into-ns [mapping-symbol]
  (symbol (str THIS_NS "/" (name mapping-symbol))))

(defn- entity-type-enum [entity-name]
  {:db/id (d/tempid :db.part/user),
   :db/ident (keyword (name entity-name))
   :db/doc "entity-type"})


(defn var-by-symbol [symbol]
  (-> symbol resolve var-get))

(defn inject-def [ns var-name value]
  (intern ns var-name value))

(defn make-var [symb val]
  (intern (symbol THIS_NS) symb val))

(defn del-var [symb]
  (ns-unmap (symbol THIS_NS) symb))

(defn get-frequencies []
  (var-by-symbol (symbol (str THIS_NS "/entities-frequencies"))))

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
     (d/transact (d/connect (or url (-> (get-schema name) :schema-opts :db-url))) schema-data)))
  ([name] (persist-schema name nil)))

(defn- do-check [key val]
  (when-not (and (symbol? key)
                 (contains? #{'name 'type 'cardinality 'mapping-opts 'mapping-hook 'reverse-mapping-hook 'index 'unique 'component?} key))
    (throw (IllegalArgumentException. (str "Wrong description of rule (" first "," val ")")))))

(defn- decode-args [args]
  (let [partitioned (partition 2 args)]
    (->> (doall (mapv (fn [p] (do-check (first p) (last p))
                        {(keyword (first p)), (last p)}) partitioned))
         (apply merge))))

(defmacro defenum [n]
  (append-schema {:db/id (d/tempid :db.part/user)
                  :db/ident (eval n)
                  :db/doc "enum"})
   identity)

(defmacro property [& args]
  (let [decoded (decode-args args)
        prop-name (:name decoded)
        entity-name (name (get-var 'curr-entity-name))
        to-property (keyword (str entity-name "/" (name (or (:to-property decoded) prop-name))))
        property-def (assoc decoded :to-property to-property)]
    (if-not (get-var 'reversed-mapping)
      (do (append-schema (create-db-property property-def))
          {(:name decoded)
           (dissoc (assoc decoded :to-property to-property) :name)})
      {to-property (merge {:to-property prop-name}
                          (when-let [hook (:reverse-mapping-hook decoded)] {:mapping-hook hook}))})))

(defn concat-into [& items]
  (into []  (-> (apply concat items)
                distinct)))

(defmacro defentity [entity-name & rules]
  (append-schema (entity-type-enum (eval entity-name)))
  (make-var 'curr-entity-name (eval entity-name))
  (let [entity-var (var-get (intern (symbol THIS_NS) (eval entity-name)
                                    {:type (eval entity-name)
                                     :mapping (do (make-var 'reversed-mapping false)
                                                  (apply merge (mapv #(eval %) rules)))
                                     :rev-mapping (do (make-var 'reversed-mapping true)
                                                      (apply merge (mapv #(eval %) rules)))}))]
    (when (-> (current-schema) :schema-opts :mapping-inference)
      (let [prop-map (apply merge (mapv (fn [k] {k [(:type entity-var)]}) (-> entity-var :mapping (keys))))]
        (alter-var-root #'entities-frequencies (fn [o] (merge-with concat-into entities-frequencies prop-map))))))
  (del-var 'curr-entity-name)
  (del-var 'reversed-mapping)
  identity)

(defmacro defschema [n opts & defentities]
  (let [options (eval opts)
        name (name (eval n))] ;; canonical representation should be string. No matter if on input there is string symbol or keyword it would be string here.
    (when (and (-> options :db-drop)
               (-> options :db-url))
      (d/delete-database (-> options :db-url)))
    (alter-var-root #'schema (fn [o] (assoc-in schema [(keyword name)] {:schema-opts options})))
    (make-var 'curr-schema name)
    (append-schema ENTITY_TYPE_ATTRIB)
    ;(doseq [deff defentities] (eval deff))
    (doall (map #(eval %) defentities))
    ;(eval defentities)
    (when (and (-> options :auto-persist-schema)
               (-> options :db-url))
      (initialize-database)
      (persist-schema name))
    identity))

(defn mapping-by-symbol [symbol]
  (-> symbol resolve var-get))

(defn- entity-less? [clj]
  (if (vector? clj) (entity-less? (first clj))
      (not (map? clj))))

(defn- entity-types [url]
  (when url
    (let [db (d/db (d/connect url))]
      (apply merge (mapv (fn [e] {(keyword (str (:db/id (first e)))),
                                  (:db/ident (first e))}) (d/q '[:find (pull ?p [*])
                                                                 :in $
                                                                 :where [?p :db/doc "entity-type"]] db))))))

(def lookup-entity-types (memoize entity-types))

(defn- resolve-entity-type [entity-type]
  (if (map? entity-type)
    (or (:db/ident entity-type)
        (when-let [db-id (:db/id entity-type)]
          (when *db-url*
            (-> (lookup-entity-types *db-url*)
                ((keyword (str db-id)))))))
    entity-type))

(defn persisted-entity-type [entity]
  (if (vector? entity)
    (let [entry (first entity)] (persisted-entity-type entry))
    (when-let [entity-type-val (:entity/type entity)]
      (resolve-entity-type entity-type-val))))

(defn find-mapping [service-entity]
  (when-not (entity-less? service-entity)
    (if-let [entity-type (persisted-entity-type service-entity)]
      (->> (name entity-type) (str THIS_NS "/") symbol (var-by-symbol))
      (let [freqs (->> (mapv #(get entities-frequencies %) (keys service-entity))
                       (apply concat)
                       (frequencies))]
        (when (< 0 (count (keys freqs)))
          (let [freqs-vec (mapv (fn [k] {:k k, :v (get freqs k)}) (keys freqs))
                max-val (apply max (mapv #(get freqs %) (keys freqs)))
                max-entries (filter #(= max-val (:v %)) freqs-vec)]
            (when (< 1 (count max-entries)) (throw (ex-info "Cannot determine mapping. At least two mappings with same frequency" {:entries max-entries})))
            (->> (first max-entries)
                 :k
                 name
                 (str THIS_NS "/")
                 symbol
                 (var-by-symbol))))))))

(def ^:private find-mapping-memoized (memoize find-mapping))

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

(defn- map-property [mapping-func type from-property to-property source mapping-rules]
  (let [property-value (from-property @source)
        mapping-opts (:mapping-opts mapping-rules)]
    (if-let [mapping-hook (:mapping-hook mapping-rules)]
      (swap! source assoc to-property (mapping-hook property-value))
      (do (make-var 'do-mapping? true)
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
          (del-var 'do-mapping?))))
  (swap! source dissoc from-property))


(defn- has? [property mapping]
  (when (not (contains? mapping property))
    (throw (ex-info "Mapping error. Source property doesn't contain corresponding mapping rule!"
                    {:property property
                     :mapping mapping}))))

(defn- delete-db-meta [source]
  (swap! source dissoc :db/id :entity/type))

(defn clip [source]
  (if (vector? source)
    (doall (map #(clip %) source))
    (when-let [mapping (find-mapping-memoized source)]
      (let [properties (-> mapping :mapping keys set)]
        (->> (remove #(contains? properties %) (keys source))
             (apply dissoc source))))))


(defn- add-db-meta [source-atom mapping]
  (swap! source-atom assoc :db/id (make-temp-id))
  (swap! source-atom assoc :entity/type (keyword (name (:type mapping)))))

(defn- do-mapping [source-atom entity-type mapping-rules mapping-func]
  (let [source-props (keys @source-atom)]
    (doall (map #(do (has? % mapping-rules)
                     (map-property mapping-func
                                   entity-type
                                   %
                                   (:to-property (% mapping-rules))
                                   source-atom
                                   (% mapping-rules))) source-props))))

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
   (mapv #(clj->db %) source)))

(defmethod clj->db :default
  ([source mapping] source)
  ([source] source))

(defmulti db->clj (fn ([source url] (type source))
                      ([source] (type source))))

(defmethod db->clj (type {})
  ([source url]
   (binding [*db-url* url]
     (if-let [mapping (find-mapping source)]
       (let [mapping-rules (:rev-mapping mapping)
             temp-source (atom source)]
         (delete-db-meta temp-source)
         (do-mapping temp-source (:type mapping) mapping-rules db->clj)
         @temp-source)
       source)))
  ([source]
   (db->clj source *db-url*)))


;;TODO: Handle mapping of collection of non entities. e.g. collection of vectors.
(defmethod db->clj (type [])
  ([source url]
   (mapv #(db->clj % url) source))
  ([source]
   (mapv #(db->clj %) source))) ;;This is wrong assumption. What about when we tries to map a vector of entities with different types ?

(defmethod db->clj :default
  ([source url] source)
  ([source] source))
