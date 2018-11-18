(ns impl.features.default
  (:require [core.entities :as e]
            [core.features :as f]
            [impl.components :as c]))

(defn has-endpoints [target]
  ((f/has-by-one-component-of-types #{::c/startpoint ::c/endpoint}) target))

(defn has-relation [target]
  ((f/has-components-of-types #{::c/relation}) target))

(defn has-controls [target]
  ((f/has-components-of-types #{::c/control}]) target))

(defn is-rectangular-entity [target]
  ((f/has-components-of-types #{::c/main}) target))

(defn is-associative-entity [target]
  (f/any-of-features [
     has-endpoints
     has-controls
  ] target))

(defn is-interactive-entity [target]
  ((f/has-components-of-types #{::c/control}]) target))

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
