(ns impl.features.default
  (:require [core.features :as f]
            [impl.components :as c]))

(defn has-endpoints [target]
  ((f/has-by-one-component-of-types #{::c/startpoint ::c/endpoint}) target))

(defn has-relation [target]
  ((f/has-components-of-types #{::c/relation}) target))

(defn has-controls [target]
  ((f/has-components-of-types #{::c/control}) target))

(defn is-rectangular-entity [target]
  ((f/has-components-of-types #{::c/entity-shape}) target))

(defn is-associative-entity [target]
  (f/any-of-features [
     has-endpoints
     has-controls
  ] target))

(defn is-interactive-entity [target]
  ((f/has-components-of-types #{::c/control}) target))

(defn is-primary-entity [target]
  (f/check-features [
     is-associative-entity
     is-rectangular-entity
  ] target ))

(defn is-association-entity [target]
  (f/check-features [
     is-associative-entity
     has-relation
  ] target ))

(defn is-selection-attribute [target]
  (let [attribute (:attribute target)]
    (not (nil? (:domain attribute)))))

(defn is-single-attribute [target]
  (let [attribute (:attribute target)]
    (nil? (:domain attribute))))

(defn is-text-attribute [target]
  (f/check-features [
     is-single-attribute
     (f/has-components-of-types #{::c/text})
  ] target ))
