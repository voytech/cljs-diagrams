(ns cljs-diagrams.impl.std.features.default
  (:require [cljs-diagrams.core.features :as f]
            [cljs-diagrams.impl.std.components :as c]))

(defn has-endpoints [target]
  ((f/has-by-one-component-of-types #{::c/startpoint ::c/endpoint}) target))

(defn has-relation [target]
  ((f/has-components-of-types #{::c/relation}) target))

(defn has-controls [target]
  ((f/has-components-of-types #{::c/control}) target))

(defn is-shape-entity [target]
  ((f/has-components-of-types #{::c/entity-shape}) target))

(defn is-associative-entity [target]
  (f/any-of-features [
                      has-endpoints
                      has-controls]
   target))

(defn is-interactive-entity [target]
  ((f/has-components-of-types #{::c/control}) target))

(defn is-primary-entity [target]
  (f/check-features [
                     is-associative-entity
                     is-shape-entity]
   target))

(defn is-container [target]
  (f/check-features [
                     (fn [target]
                       (->> (:tags target)
                            (filterv #(= % :container))
                            count
                            (= 1)))
                     is-shape-entity]
   target))

(defn is-association-entity [target]
  (f/check-features [
                     is-associative-entity
                     has-relation]
   target))

(defn is-selection-attribute [target]
  (let [attribute (:attribute target)]
    (not (nil? (:domain attribute)))))

(defn is-single-attribute [target]
  (let [attribute (:attribute target)]
    (nil? (:domain attribute))))

(defn is-text-attribute [target]
  (f/check-features [
                     is-single-attribute
                     (f/has-components-of-types #{::c/text})]
   target))
