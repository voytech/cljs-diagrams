(set-env!
  :project      'photo-collage
  :version      "0.1.0-SNAPSHOT"
  :dependencies '[
                  [tailrecursion/hoplon      "6.0.0-SNAPSHOT"]
                  [adzerk/boot-cljs "0.0-2814-1" :scope "test"]
                  [adzerk/boot-cljs-repl "0.1.9"]
                  [tailrecursion/boot-hoplon "0.1.0-SNAPSHOT" :scope "test"]
                  [tailrecursion/javelin     "3.7.2"]
                  [io.hoplon.vendor/jquery   "2.1.1-0"]
                  [com.cemerick/clojurescript.test "0.3.3"]
                  [io.hoplon/twitter.bootstrap "0.2.0"]]
  :asset-paths    #{"assets"}
  :out-path       "target"
  :cljs-out-path  "tests"
  :source-paths   #{"src"}
  :cljs-runner   "\\test-runner\\runner.js"
  :src-paths    #{"src/clj" "src/cljs" "src/hoplon" "src/test"})



(require
  '[tailrecursion.boot-hoplon :refer :all]
  '[cemerick.cljs.test :refer :all]
  '[clojure.java.shell :refer [sh]]
  '[adzerk.boot-cljs :refer :all]
  '[adzerk.boot-cljs-repl :refer :all]
  )

(deftask clojurescript-compile []
  (comp  (cljs :unified true
               :source-map false
               :optimizations :none)))

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
