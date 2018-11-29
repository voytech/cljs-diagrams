(set-env!
 :source-paths    #{"src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-2"  :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.3"      :scope "test"]
                 [adzerk/boot-reload        "0.4.13"     :scope "test"]
                 [adzerk/boot-test          "1.2.0"]
                 [pandeiro/boot-http        "0.8.0"]
                 [tolitius/boot-check       "0.1.9"]
                 [com.cemerick/piggieback   "0.2.1"      :scope "test"]
                 [org.clojure/tools.nrepl   "0.2.13"     :scope "test"]
                 [weasel                    "0.7.0"      :scope "test"]
                 [org.clojure/clojurescript "1.9.293"] ;;293 562
                 [org.clojure/core.async    "0.3.442"]
                 [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                 [reagent "0.6.0"]
                 [binaryage/devtools "0.9.0" :scope "test"]
                 [binaryage/dirac "1.2.17" :scope "test"]
                 [powerlaces/boot-cljs-devtools "0.2.0" :scope "test"]
                 [cljsjs/fabric                   "1.5.0-0"]
                 [cljsjs/jquery                   "2.1.4-0"]
                 [cljsjs/jquery-ui                "1.11.3-1"]
                 [cljsjs/rx                       "4.0.7-0"]])

(require
 '[adzerk.boot-cljs       :refer [cljs]]
 '[tolitius.boot-check    :as check]
 '[tolitius.reporter.html :refer :all]
 '[adzerk.boot-cljs-repl  :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload     :refer [reload]]
 '[pandeiro.boot-http     :refer [serve]]
 '[adzerk.boot-test]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools dirac]])

(deftask build []
  (comp (speak)
        (cljs)))

(deftask analyze []
  (set-env! :source-paths #{"src/cljs"})
  (comp
    (check/with-eastwood :options {:gen-report true})
    (check/with-bikeshed :options {:gen-report true})
    (check/with-kibit :options {:gen-report true})))


(deftask run []
  (comp (serve)
        (watch)
        (cljs-repl)
        (cljs-devtools)
        (dirac)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none}
                 reload {:on-jsload 'app.app/init})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)
        (target :dir #{"target"})))


(deftask testing []
  (set-env! :source-paths #(conj % "test/cljs"))
  identity)

;;; This prevents a name collision WARNING between the test task and
;;; clojure.core/test, a function that nobody really uses or cares
;;; about.
(ns-unmap 'boot.user 'test)

(deftask test []
  (comp (testing)
        (test-cljs :js-env :phantom
                   :exit?  true)))

(deftask auto-test []
  (comp (testing)
        (watch)
        (test-cljs :js-env :phantom)))

(task-options!
   serve {:dir "target"})
