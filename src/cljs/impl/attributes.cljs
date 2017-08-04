(ns impl.attributes
 (:require [core.entities :as e]
           [core.project :as p])
 (:require-macros [core.macros :refer [defattribute]]))

(defattribute name
              {:cardinality 1
               :weight 1
               :create (fn [attribute data]
                        (e/create-attribute-value
                          attribute
                          data
                          nil
                          {:attribute (e/EntityDrawable "name" :name (js/fabric.Text. "Name") [])
                           :value (e/EntityDrawable "val" :value (js/fabric.Text. data) [])}))
               :sync (fn [value]
                        (.setText (:src (:value (:drawables value))) (:value value)))})
