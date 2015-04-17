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
                  [clj-tagsoup                     "0.3.0"]
                  [voytech/boot-clojurescript.test "0.1.0-SNAPSHOT"]]
  ;;:asset-paths    #{"assets"}
  :out-path       "resources/public"
  :target-path    "resources/public"
  :source-paths   #{"src/cljs" "src/main/clj" "src/hoplon"} ;;Echh still need to refactor to src/main/clj src/main/hoplon
  :test-paths     #{"src/test/cljs" }
  :resource-paths #{"assets"}
  )


(require
  '[tailrecursion.boot-hoplon :refer :all]
  '[boot.util :refer [sh]]
  '[cemerick.cljs.test :refer :all]
  '[adzerk.boot-cljs :refer :all]
  '[adzerk.boot-cljs-repl :refer :all]
  '[adzerk.boot-reload    :refer [reload]]
  '[pandeiro.boot-http :refer [serve]]
  '[boot.core          :as boot]
  '[boot-clojurescript.test.tasks :refer :all]
  )


(deftask continous
  []
  (comp (serve) (watch) (speak) (hoplon) (reload) (cljs :source-map true)))

(deftask dev
  []
  (comp (hoplon) (cljs)))

;;This task should give also reload and cljs-repl
(deftask test-driven-dev
 []
 (comp (serve)
       (watch)
       (hoplon)
       (reload)
     ;;(cljs-repl)
       (cljs-tests))) ;;task cljs-tests will also compile clojurescript automatically make shim for runnable SPA.


(deftask tests
  "Run clojurescript.test tests"
  []
  (comp (watch) (cljs-tests))
)

(task-options!
 serve {
        :dir "resources/public"
        })
