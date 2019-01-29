(ns core.utils.svg
  (:require [core.utils.dom :as dom]
            [clojure.string :as str]))

(defn create-svg [parent-dom-id element-suffix attributes]
  (let [container (dom/by-id parent-dom-id)
        element (dom/create-ns-node "http://www.w3.org/2000/svg" "svg" (str parent-dom-id "-" element-suffix))]
     (dom/append-child container element)
     (dom/attrs element attributes)
     element))

(defn create-element [type svg-root attributes]
 (let [rect (dom/create-ns-node "http://www.w3.org/2000/svg" type)]
   (dom/attrs rect attributes)
   (dom/append-child svg-root rect)
   rect))

(defn create-rect [svg-root attributes]
  (create-element "rect" svg-root attributes))

(defn create-text [svg-root attributes]
  (create-element "text" svg-root attributes))

(defn create-textarea [svg-root attributes]
  (let [g (create-element "g" svg-root {})]
    (create-element "text" g attributes)
    g))

(defn create-circle [svg-root attributes]
  (create-element "circle" svg-root attributes))

(defn create-image [svg-root attributes]
  (create-element "image" scg-root attributes))

(defn create-line [svg-root attributes]
  (create-element "line" scg-root attributes))

(defn create-path [svg-root attributes]
  (create-element "path" scg-root attributes))

(defn tokenize [text]
  (str/split text #"\s+"))

(defn is-propagatable [attrib]
  (and (not= "x" attrib)
       (not= "y" attrib)
       (not= "width" attrib)
       (not= "height" attrib)
       (not= "data-text" attrib)))

(defn propagatable-attributes [g]
  (let [attributes (.-attributes g)
        count (.-length attributes)]
    (apply merge  (->> (range count)
                       (mapv #(aget attributes %))
                       (filterv #(is-propagatable (.-name %)))
                       (mapv (fn [attr] {(.-name attr) (.-value attr)}))))))
                       
(defn propagate-attributes [elem attribs]
  (let [count (dom/child-count elem)]
    (doseq [child (mapv #(dom/get-child-at elem %) (range count))]
      (doseq [attribkey (fiterv is-propagatable (keys attribs))]
        (dom/attr child attribkey (get attribs attribkey))))))

(defn propagate-attribute [elem attrib val]
  (when (is-propagatable attrib)
    (let [count (dom/child-count elem)]
      (doseq [child (mapv #(dom/get-child-at elem %) (range count))]
        (dom/attr child attrib val)))))

(defn layout-multiline-text [svg])

(defn set-multiline-text [svg]
  (let [text (dom/attr svg "data-text")
        orig-x (dom/attr svg "data-x")
        orig-y (dom/attr svg "data-y")
        x (volatile! orig-x)
        y (volatile! orig-y)
        width (dom/attr svg "data-width")
        height (dom/attr svg "data-height")
        words (tokenize text)]
    (dom/remove-childs svg)
    (doseq [word words]
      (let [attribs   (merge (propagatable-attributes svg) {"x" @x "y" @y})
            word-node (create-text svg attribs)]
        (dom/set-text word-node (str word " "))
        (let [bbox (.getBBox word-node)
              wn-width (.-width bbox)
              wn-height (.-height bbox)]
          (if (> (+ width orig-x) (+ wn-width @x))
            (vreset! x (+ wn-width @x))
            (do (vreset! x orig-x)
                (vreset! y (+ @y wn-height)))))))))

(defn set-singleline-text [svg]
  (dom/set-text svg (dom/attr svg "data-text")))

(defn set-text-internal [svg]
  (if (= "true" (dom/attr svg "data-multiline-text"))
    (set-multiline-text svg)
    (set-singleline-text svg)))

(defn is-word-wrap [name val]
  (and (= :multiline-text (keyword name))
       (true? val)))

(defn is-g [elem]
  (or (= "g" (.-nodeName elem))
      (= "G" (.-nodeName elem))))

(defn g-attr [elem name val]
  (if (and (is-g elem)
           (not= "id" name))
    (do (dom/attr elem (str "data-" name) val)
        (propagate-attribute elem name val))
    (dom/attr elem name val)))

(defn svg-attr [elem name val]
  (g-attr elem name val)
  (when (is-word-wrap name val)
    (set-text-internal elem)))

(defn svg-set-text [elem text]
  (dom/attr elem "data-text" text)
  (set-text-internal elem))
