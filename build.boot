(set-env!
  :project      'photo-collage
  :version      "0.1.0-SNAPSHOT"
  :dependencies '[[boot/core                       "2.0.0-rc10"]
                  [adzerk/bootlaces                "0.1.10" :scope "test"]
                  [tailrecursion/hoplon            "6.0.0-SNAPSHOT"]
                  [adzerk/boot-cljs                "0.0-2814-3" ]
                  [adzerk/boot-cljs-repl           "0.1.9"]
                  [adzerk/boot-reload              "0.2.6"]
                  [pandeiro/boot-http              "0.6.2"]
                  [tailrecursion/boot-hoplon       "0.1.0-SNAPSHOT"]
                  [tailrecursion/javelin           "3.7.2"]
                  [io.hoplon.vendor/jquery         "2.1.1-0"]
                  [com.cemerick/clojurescript.test "0.3.3"]
                  ;;[hoplon/twitter-bootstrap        "0.1.0"] ;; This will be set if package is released.
                  [clj-tagsoup                     "0.3.0"]]
  ;;:asset-paths    #{"assets"}
  :out-path       "resources/public"
  :target-path    "resources/public"
  :cljs-out-path  "tests"
  :source-paths   #{"src/cljs" "src/test" "src/hoplon"}
  :resource-paths #{"assets"}
  :cljs-runner    "\\test-runner\\runner.js"
  :src-paths      #{"src/clj" "src/cljs" "src/hoplon" "src/test"})



(require
  '[clojure.walk :refer :all]
  '[tailrecursion.boot-hoplon :refer :all]
  '[boot.util :refer [sh]]
  '[cemerick.cljs.test :refer :all]
  '[adzerk.boot-cljs :refer :all]
  '[adzerk.boot-cljs.util       :as util]
  '[adzerk.boot-cljs.middleware :as wrap]
  '[adzerk.boot-cljs.js-deps    :as deps]
  '[adzerk.boot-cljs.impl :refer :all]
  '[adzerk.boot-cljs-repl :refer :all]
  '[adzerk.boot-reload    :refer [reload]]
  '[pandeiro.boot-http :refer [serve]]
  '[boot.core                      :as boot]
  '[boot.file                   :as file]
  '[boot.pod                       :as pod]
  '[clojure.java.io                :as io]
  '[clojure.pprint              :as pp]
  '[tailrecursion.boot-hoplon.haml :as haml]
  '[tailrecursion.boot-hoplon.impl :as impl]
  )

(defn- compile-no-pod
  [{:keys [tmp-src tmp-out main files opts] :as ctx} ]
  (info "Compiling %s...\n" (-> opts :output-to util/get-name))
  (dbug "CLJS options:\n%s\n" (with-out-str (pp/pprint opts)))
  (let [
        files (:dir (first (:cljs files)))
        {:keys [warnings dep-order]}
        (adzerk.boot-cljs.impl/compile-cljs [(.getPath tmp-src)
                                             (.getPath files)] opts)]     ;;Here added fiels to compile !
    (swap! boot/*warnings* + (or warnings 0))
    (concat dep-order [(-> opts :output-to util/get-name)])))

(defn- set-output-dir-opts
  [{:keys [output-dir] :as opts} tmp-out]
  (->> (doto (io/file tmp-out output-dir) .mkdirs) .getPath (assoc opts :output-dir)))

(defn- initial-context
  "Create initial context object. This is the base configuration that each
  individual compile starts with, constructed using the options provided to
  the task constructor."
  [tmp-out tmp-src tmp-result task-options]
  {:tmp-out    tmp-out
   :tmp-src    tmp-src
   :tmp-result tmp-result
   :main       nil
   :files      nil
   :opts       (-> {:libs          []
                    :externs       []
                    :preamble      []
                    :output-dir    "out"
                    :optimizations :none}
                   (into (:compiler-options task-options))
                   (merge (dissoc task-options :compiler-options))
                   (set-output-dir-opts tmp-out))})

(defn- prep-context
  "Add per-compile fields to the base context object."
  [main files docroot ctx]
  (assoc ctx :main main :files files :docroot docroot))

(defn- prep-compile
  "Given a per-compile base context, applies middleware to obtain the final,
  compiler-ready context for this build."
  [{:keys [tmp-src tmp-out main files opts] :as ctx}]
  (util/delete-plain-files! tmp-out)
  (util/delete-plain-files! tmp-src)
  (->> ctx wrap/main wrap/level wrap/shim wrap/externs wrap/source-map))

(defn- copy-to-docroot
  "Copies everything the application needs, relative to its js file."
  [docroot {:keys [tmp-out tmp-result] {:keys [incs]} :files}]
  (util/copy-docroot! tmp-result docroot tmp-out)
  (doseq [[p f] (map (juxt boot/tmppath boot/tmpfile) incs)]
    (file/copy-with-lastmod f (io/file tmp-result (util/rooted-file docroot p)))))

(boot/deftask ^:private default-main
  "Private task---given a base compiler context creates a .cljs.edn file and
  adds it to the fileset if none already exist. This default .cljs.edn file
  will :require all CLJS namespaces found in the fileset."
  [c context CTX edn "The cljs compiler context."]
  (let [tmp-main (boot/temp-dir!)]
    (boot/with-pre-wrap fileset
      (boot/empty-dir! tmp-main)
      (let [{:keys [cljs main]} (deps/scan-fileset fileset)]
        (if (seq main)
          fileset
         (let [output-to (or (get-in context [:opts :output-to]) "main.js")
                out-main  (-> output-to (.replaceAll "\\.js$" "") deps/add-extension)
                out-file  (doto (io/file tmp-main out-main) io/make-parents)]
            (info "Writing %s...\n" (.getName out-file))
            (->> cljs
                 (mapv (comp symbol util/path->ns boot/tmppath))
                 (assoc {} :require)
                 (spit out-file))
            (-> fileset (boot/add-source tmp-main) boot/commit!)))))))


(deftask cljs-no-pod
  [O optimizations LEVEL   kw   "The optimization level."
   s source-map            bool "Create source maps for compiled JS."
   c compiler-options OPTS edn  "Options to pass to the Clojurescript compiler."]

  (let [tmp-src    (boot/temp-dir!)
        tmp-out    (boot/temp-dir!)
        tmp-result (boot/temp-dir!)
        ctx        (initial-context tmp-out tmp-src tmp-result *opts*)]

    (warn-on-cljs-version-differences)
    (comp
     (default-main :context ctx)
     (boot/with-pre-wrap fileset
         (let [{:keys [main incs cljs] :as fs} (deps/scan-fileset fileset)]
         (loop [[m & more] main, dep-order nil]
           (let [{{:keys [optimizations]} :opts :as ctx} ctx]
             (if m
               (let [docroot   (.getParent (io/file (boot/tmppath m)))
                     ctx       (prep-compile (prep-context m fs docroot ctx))
                     dep-order (->> (compile-no-pod ctx)
                                    (map #(.getPath (util/rooted-file docroot %)))
                                    (concat dep-order))]
                 (copy-to-docroot docroot ctx)
                 (recur more dep-order))
               (-> fileset
                   (boot/add-resource tmp-result)
                   (boot/add-meta (-> fileset (deps/external incs) (deps/compiled dep-order)))
                    boot/commit!)))))))))

(deftask development-watch-reload
  []
  (comp (watch) (speak) (hoplon) (reload) (cljs-no-pod)))

(deftask development-simple
  []
  (comp (hoplon) (cljs-no-pod)))

(deftask cljs-test
  "Run clojurescript.test tests"
  []
  (let [current-dir (System/getProperty "user.dir"),
        slimer-home (System/getenv "SLIMER_HOME")]
    (let [result (sh (str slimer-home "\\slimerjs.bat")
                     (str current-dir (get-env :cljs-runner))
                     (str current-dir "\\" (get-env :out-path)
                          "\\" (get-env :cljs-out-path)
                          "\\main.js"))]
      (println (:out result))
      ))
  identity
  )
