(ns ui.components.tools
  (:require [reagent.core :as reagent :refer [atom]]
            [core.utils.dnd :as dnd]
            [core.resources :as resources]
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
        :on-drag-start #(dnd/set-dnd-data (.-nativeEvent %) "tool-data" (:uid tool) "move")}
      [:div {:style {:height "50%"
                     :width "100%"
                     :top "0px"
                     :position "relative"
                     :background-image (str "url(" icon ")")
                     :background-repeat "no-repeat"
                     :background-size "contain"
                     :background-position "center"}}]
      [:div {:class "tool-title" :style {:height "50%"
                                         :position "relative"
                                         :width "100%"
                                         :text-align "center"
                                         :font-size 12
                                         :bottom "0px"}} (str name)]])

(defn ToolBox [tool-type]
  [:div {:class "thumbs-container"}
    (for [tool (if (nil? tool-type) (vals @tools) (t/by-type tool-type))]
      [ToolView tool])])
