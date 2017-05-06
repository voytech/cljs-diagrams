(ns ui.components.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dnd :as dnd]
            [core.tools :as t :refer [tools]]))

(declare select-files!)

(defn dragover [event]
  (.stopPropagation event)
  (.preventDefault event))

(defn upload-file [file callback]
  (let [reader (js/FileReader.)]
    (set! (.-onload reader) #(let [data (.-result (.-target %))
                                   result-map (atom {})
                                   img-type (re-matches #"image.*" (.-type file))]
                               (swap! result-map assoc :name (.-name file)
                                                       :type (.-type file)
                                                       :last-modified (.-lastModifiedDate file)
                                                       :size (.-size file)
                                                       :content data)
                               (if (not (nil? img-type))
                                 (callback @result-map))))
    (.readAsDataURL reader file)))

(defn drop-item [event handle]
  (.stopPropagation event)
  (.preventDefault event)
  (dnd/data-transfer event))

(defn select-files [event callback]
  (let [files (.-files (.-target event))]
      (let [file (aget files 0)]
        (upload-file file callback))))

(defn ImageLoader [callback]
  [:div {:class "image-loader"
         :on-drop #(drop-item % callback)
         :on-drag-over dragover}
    [:div {:class "file-load-wrapper"}
      [:input {:type "file"
               :name "files[]"
               :class "filestyle"
               :on-change #(select-files % callback)}]]])

(defn ToolView [{:keys [name desc type icon generator] :as tool}]
 [:div {:class "photo-thumbnail"
        :draggable "true"
        :style (str "background-image :url(" icon ");"
                    "background-repeat: no-repeat;"
                    "background-size:contain;"
                    "background-position:center;")
        :on-drag-start #(dnd/set-dnd-data % "tool-data" (:uid tool) "move")}
      [:div {:class "tool-title"} (str name)]])

(defn ToolBox [tool-type]
  [:div {:class "thumbs-container"}
    (for [tool (if (nil? tool-type) @tools (t/by-type tool-type))] [ToolView tool])])

(defn ToolBoxWithUpload [tool-type]
  [:div {:id (str tool-type "-id")}
    [ImageLoader #(js/console.log (str (:name %) (:type %) (:content %)))]
    [ToolBox tool-type]])
