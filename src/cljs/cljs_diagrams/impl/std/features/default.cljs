(ns cljs-diagrams.impl.std.features.default
  (:require [cljs-diagrams.core.features :as f]
            [cljs-diagrams.impl.std.shapes :as c]))

(defn has-endpoints [target]
  ((f/has-by-one-shape-of-types #{::c/startpoint ::c/endpoint}) target))

(defn has-relation [target]
  ((f/has-shapes-of-types #{::c/relation}) target))

(defn has-controls [target]
  ((f/has-shapes-of-types #{::c/control}) target))

(defn has-node-shape [target]
  ((f/has-shapes-of-types #{::c/node-shape}) target))

(defn is-associative-node [target]
  (f/any-of-features [
                      has-endpoints
                      has-controls]
   target))

(defn is-interactive-node [target]
  ((f/has-shapes-of-types #{::c/control}) target))

(defn is-primary-node [target]
  (f/check-features [
                     is-associative-node
                     has-node-shape]
   target))

(defn is-container [target]
  (f/check-features [
                     (fn [target]
                       (->> (:tags target)
                            (filterv #(= % :container))
                            count
                            (= 1)))
                     has-node-shape]
   target))

(defn is-association-node [target]
  (f/check-features [
                     is-associative-node
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
                     (f/has-shapes-of-types #{::c/text})]
   target))
