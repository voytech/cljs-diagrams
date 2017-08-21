(ns core.entities-test
  (:require-macros [cljs.test :refer [deftest testing is async use-fixtures]])
  (:require [cljs.test]
            [core.eventbus :as bus]
            [core.entities :as e]
            [core.drawables :as d]
            [impl.drawables :as dimpl]
            [core.drawables :as drawables]))

(defonce mock_attribute  (e/Attribute. "name" 1 1 nil {:width 100 :height 50} (fn [data] [(core.entities/Component. "name-text" :name (dimpl/text {:text data}) {})])))

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
    (is (= 3 (count (vals @e/lookups))))
    (is (= {:component "start" :entity (:uid entity)} (get @e/lookups (:uid start))))
    (is (= {:name "end" :type :end :drawable end :props {}} (e/get-entity-component entity "end")))
    (is (= {:name "start" :type :start :drawable start :props {}} (e/get-entity-component entity "start")))
    (is (= {:name "link" :type :link :drawable link :props {}} (e/get-entity-component entity "link")))
    (is (= entity (e/lookup start :entity)))
    (is (= {:name "start" :type :start :drawable start :props {}} (e/lookup start :component)))))

(deftest test-add-components []
  (let [start (dimpl/circle)
        end   (dimpl/circle)
        link  (dimpl/line)
        start-component {:name "start" :type :start :drawable start :props {}}
        end-component   {:name "end"   :type :end   :drawable end   :props {}}
        link-component  {:name "link"  :type :link  :drawable link  :props {}}
        entity (e/create-entity "relation-link")]
     (is (= 1 (count (vals @e/entities))))
     (is (= 0 (count (vals @e/lookups))))
     (e/add-entity-component entity start-component end-component link-component)
     (is (= 3 (count (vals @e/lookups))))))

(deftest test-create-attribute []
  (let [attribute mock_attribute]
      (e/add-attribute attribute)
      (is (= 1 (count (vals @e/attributes))))))

(deftest test-create-attribute-value []
  (let [attribute mock_attribute]
    (e/add-attribute attribute)
    (is (= 1 (count (vals @e/attributes))))
    (let [attribute-value (e/create-attribute-value attribute "Hello World!" {})
          drawable (e/get-attribute-value-drawable attribute-value "name-text")]
      (is (= "Hello World!" (d/getp drawable :text)))
      (is (= "Hello World!" (e/get-attribute-value-data attribute-value)))
      (is (= "Hello World!" (e/get-attribute-value-property attribute-value "name-text" :text))))))
