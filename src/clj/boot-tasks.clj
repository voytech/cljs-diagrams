(ns boot-tasks
  (:require
   [tailrecursion.boot-hoplon :refer :all]
   [boot.util :refer [sh]]
   [adzerk.boot-cljs :refer :all]
   [boot.core                      :as boot]
   [boot.pod                       :as pod]
   [clojure.java.io                :as io]
   [tailrecursion.boot-hoplon.haml :as haml]
   [tailrecursion.boot-hoplon.impl :as impl])

(boot/deftask hoplon-no-pod
  [pp pretty-print bool "Pretty-print CLJS files created by the Hoplon compiler."
   l  lib          bool "Include produced cljs in the final artefact."]
  (println (str "Boot options " *opts*))
  (let [prev-fileset (atom nil)
        tmp-cljs     (boot/temp-dir!)
        tmp-html     (boot/temp-dir!)
        opts         (dissoc *opts* :lib)
        add-cljs     (if lib boot/add-resource boot/add-source)]
    (println (str "tmp clojurescript: " tmp-cljs))
    (println (str "tmp html file" tmp-html))
    (boot/with-pre-wrap fileset
      (let [hl (->> fileset
                    (boot/fileset-diff @prev-fileset)
                     boot/input-files
                    (boot/by-ext [".hl"])
                    (map (juxt boot/tmppath (comp (memfn getPath) boot/tmpfile))))]
        (reset! prev-fileset fileset)
        (impl/hoplon (.getPath tmp-cljs) (.getPath tmp-html) hl opts)
        )
       (-> fileset (add-cljs tmp-cljs) (boot/add-resource tmp-html) boot/commit!)
)))

(boot/deftask cljs-no-pod []

)
