(ns utils.fonts)

(def fonts (atom ["Times New Roman"
                  "Courier"
                  "Calibri"]))

(defn available-fonts []
  @fonts
)

(defn reg-font-name [])
