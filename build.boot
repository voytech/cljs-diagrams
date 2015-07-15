(set-env!
  :project      'photo-collage
  :version      "0.1.0-SNAPSHOT"
  :dependencies '[[boot/core                       "2.0.0-rc10"] ;;rc10
                  [adzerk/bootlaces                "0.1.10" :scope "test"]
                  [tailrecursion/hoplon            "6.0.0-SNAPSHOT"]
                  [adzerk/boot-cljs                "0.0-2814-3" ]
                  [adzerk/boot-reload              "0.2.6"]
                  [pandeiro/boot-http              "0.6.2"]
                  [tailrecursion/boot-hoplon       "0.1.0-SNAPSHOT"]
                  [tailrecursion/javelin           "3.7.2"]
                  [io.hoplon.vendor/jquery         "2.1.1-0"]
                  [com.cemerick/clojurescript.test "0.3.3"]
                  [com.datomic/datomic-free        "0.8.4159" :exclusions [commons-codec]] ; this clashes with castra.
                  [clj-tagsoup                     "0.3.0"]
                  [tailrecursion/castra            "2.2.2"]
                  [ring/ring                       "1.3.2"]
                  [ring/ring-core                  "1.3.2"]
                  [ring/ring-jetty-adapter         "1.3.2"]
                  [voytech/boot-clojurescript.test "0.1.0-SNAPSHOT"]]
  ;;:asset-paths    #{"assets"}
  :out-path       "resources/public"
  :target-path    "resources/public"
  :source-paths   #{"src/cljs" "src/clj" "src/hoplon"} ;;Echh still need to refactor to src/main/clj src/main/hoplon
  :test-paths     #{"src/test/cljs" }
  :resource-paths #{"assets"}
  )


(require
  '[tailrecursion.boot-hoplon :refer :all]
  '[boot.util :refer [sh]]
  '[cemerick.cljs.test :refer :all]
  '[adzerk.boot-cljs :refer :all]
  '[adzerk.boot-reload    :refer [reload]]
  '[pandeiro.boot-http :refer [serve]]
  '[boot.core          :as boot]
  '[boot-clojurescript.test.tasks :refer :all]
  '[tailrecursion.castra.handler :as h :refer [castra]]
  '[app :refer [run-app start running]]
  )


(deftask start-server-task [n namespace    SYM  sym  "The castra handler(s) to serve."
                     d docroot       PATH str  "The directory to serve."
                     p port          PORT int  "The port to listen on. (Default: 8000)"
                     j join          JOIN bool "Wait for server."]
  (let [join? (or join false)
        port! (or port 8080)
        path  (or docroot "resources/public" )]
    (boot/with-pre-wrap fileset
      ;verify if it is running then just skip.
      (when (not running) (start port! path namespace join?))
      fileset)))

(deftask continous
  []
  (comp (serve)
        (watch)
        (hoplon)
        (reload)
        (cljs :source-map true)
        ))

(deftask dev
  []
  (comp (hoplon) (cljs)))

(deftask dev-server []
  (comp (watch)
        (hoplon)
        (cljs :source-map true)
        (start-server-task :namespace 'core.api :port 3000 :join false)))

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
