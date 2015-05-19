(ns utils.fonts)

(def ^:private font-families (atom ["Times New Roman"
                  "Courier"
                  "Calibri"]))
(def ^:private font-sizes (atom (map #(str %) (range 8 50))))

(def ^:private font-weights (atom ["normal" "bold" "bolder" "lighter"]))

(defn- scan-fonts [])

(defn available-fonts []
  @font-families
)

(defn available-font-sizes []
  @font-sizes
)

(defn available-font-weights []
 @font-weights
)

(defn reg-font-name [name] (swap! font-families conj name))
