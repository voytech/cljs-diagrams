(set-env!
  :project      'photo-collage
  :version      "0.1.0-SNAPSHOT"
  :dependencies '[[boot/core "2.0.0-rc10"]
                  [adzerk/bootlaces  "0.1.10" :scope "test"]
                  [tailrecursion/hoplon      "6.0.0-SNAPSHOT"]
                  [adzerk/boot-cljs "0.0-2814-3" :scope "test"]
                  [adzerk/boot-cljs-repl "0.1.9"]
                  [tailrecursion/boot-hoplon "0.1.0-SNAPSHOT" :scope "test"]
                  [tailrecursion/javelin     "3.7.2"]
                  [io.hoplon.vendor/jquery   "2.1.1-0"]
                  [com.cemerick/clojurescript.test "0.3.3"]
                  [io.hoplon/twitter.bootstrap "0.2.0"]
                  [clj-tagsoup "0.3.0"]]
  ;;:asset-paths    #{"assets"}
  :out-path       "target"
  :cljs-out-path  "tests"
  :source-paths   #{"src/cljs" "src/test" "src/hoplon"}
  :cljs-runner   "\\test-runner\\runner.js"
  :src-paths    #{"src/clj" "src/cljs" "src/hoplon" "src/test"})



(require
  '[tailrecursion.boot-hoplon :refer :all]
  '[boot.util :refer [sh]]
  '[cemerick.cljs.test :refer :all]
  '[adzerk.boot-cljs :refer :all]
  '[adzerk.boot-cljs-repl :refer :all]
  '[boot.core                      :as boot]
  '[boot.pod                       :as pod]
  '[clojure.java.io                :as io]
  '[tailrecursion.boot-hoplon.haml :as haml]
  '[tailrecursion.boot-hoplon.impl :as impl]
  )

(deftask hoplon-no-pod
  [pp pretty-print bool "Pretty-print CLJS files created by the Hoplon compiler."
   l  lib          bool "Include produced cljs in the final artefact."]
  (println (str "Boot options " *opts*))
  (let [prev-fileset (atom nil)
        tmp-cljs     (boot/temp-dir!)
        tmp-html     (boot/temp-dir!)
        opts         (dissoc *opts* :lib)
        add-cljs     (if lib boot/add-resource boot/add-source)]
    (println tmp-cljs)
    (println tmp-html)
    (boot/with-pre-wrap fileset
      (let [hl (->> fileset
                    (boot/fileset-diff @prev-fileset)
                     boot/input-files
                    (boot/by-ext [".hl"])
                    (map (juxt boot/tmppath (comp (memfn getPath) boot/tmpfile))))]
        (reset! prev-fileset fileset)
        (impl/hoplon (.getPath tmp-cljs) (.getPath tmp-html) hl opts)
        )
        fileset
      ;; (-> fileset (add-cljs tmp-cljs) (boot/add-resource tmp-html) boot/commit!)
)))



(deftask hoplon-compile []
  (comp  (cljs :unified true
               :source-map false
               :optimizations :none) (hoplon)))

(deftask development
  "Build photo-collage for development."
  []
  (comp (watch) (hoplon {:pretty-print true :prerender false}) ;;(dev-server :port 3333)
        ))

(deftask dev-debug
  "Build photo-collage for development with source maps."
  []
  (comp (watch) (hoplon {:pretty-print true
                         :prerender false
                         :source-map true}) ;;(dev-server :port 3333)
        ))

(deftask production
  "Build photo-collage for production."
  []
  (hoplon {:optimizations :advanced}))

(deftask cljs-compile
  "Simple clojurescript compiler task.. "
  []
  (cljs :output-path  "/tests/main.js" ))

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
