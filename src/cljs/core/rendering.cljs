(ns core.rendering)

(def RENDERER (atom :canvas))

(defn set-renderer [renderer]
  (reset! RENDERER renderer))

(defn get-renderer []
  @RENDERER)

(defmulti create (fn [type options] [@RENDERER type]))

(defmulti update (fn [source model] @RENDERER))

(defmulti resolve-property (fn [known-property] [@RENDERER known-property]))

(defmulti set (fn [source property value] @RENDERER))

(defmulti get (fn [source property] @RENDERER))

(defmulti set-left (fn [source value] @RENDERER))

(defmulti set-top (fn [source value] @RENDERER))

(defmulti set-width (fn [source value] @RENDERER))

(defmulti set-height (fn [source value] @RENDERER))

(defmulti set-border-style (fn [source value] @RENDERER))

(defmulti set-border-color (fn [source value] @RENDERER))

(defmulti set-border-width (fn [source value] @RENDERER))

(defmulti set-opacity (fn [source value] @RENDERER))

(defmulti set-visible (fn [source value] @RENDERER))
