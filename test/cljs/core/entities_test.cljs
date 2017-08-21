(ns core.entities-test
  (:require-macros [cljs.test :refer [deftest testing is async use-fixtures]])
  (:require [cljs.test]
            [core.eventbus :as bus]
            [core.entities :as e]
            [impl.drawables :as dimpl]
            [core.drawables :as drawables]))

(defn cleanup [f]
  (reset! e/lookups {})
  (reset! e/entities {})
  (reset! e/drawables {})
  (f)
  (reset! e/lookups {})
  (reset! e/entities {})
  (reset! e/drawables {}))

(use-fixtures :each cleanup)

(deftest test-define-lookup []
  (e/define-lookup "123456789" {:entity "1111" :component "relation"})
  (is (= 1 (count (vals @e/lookups))))
  (is (= {:entity "1111" :component "relation"} (get @e/lookups "123456789"))))

(deftest test-create-entity []
  (let [start (dimpl/circle)
        end   (dimpl/circle)
        link  (dimpl/line)
        entity (e/create-entity "relation-link" [{:name "end"   :type :end   :drawable end   :props {}}
                                                 {:name "start" :type :start :drawable start :props {}}
                                                 {:name "link"  :type :link  :drawable link  :props {}}])]
    (is (= 1 (count (vals @e/entities))))
    (is (= {:name "end" :type :end :drawable end :props {}} (e/get-entity-component entity "end")))
    (is (= {:name "start" :type :end :drawable start :props {}} (e/get-entity-component entity "start")))
    (is (= {:name "link" :type :end :drawable link :props {}} (e/get-entity-component entity "link")))))
