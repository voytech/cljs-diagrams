(ns impl.behaviours.editors
 (:require [core.entities :as e]
           [impl.behaviours.standard-api :as behaviours]
           [core.project :as p]
           [impl.drawables :as di]
           [core.drawables :as d]
           [impl.components :as c]
           [core.behaviours :as b]
           [core.events :as ev]
           [core.options :as o]))

(defn- append-state [type element event]
  (p/append-state type {:element element
                        :entity (:entity event)
                        :attribute-value (:attribute-value event)}))

(defn- get-state [type]
  (p/get-state type))

(defmulti create-element (fn [type av] type))

(defmulti get-element-value (fn [type editor] type))

(defmulti set-element-value (fn []))

(defmethod create-element :editor [type av]
  (let [input (js/document.createElement "input")]
    (aset input "value" (e/get-attribute-value-data av))
    input))

(defmethod create-element :domain-editor [type av]
  (let [select (js/document.createElement "select")
        domain (-> av :attribute :domain)]
    (doseq [entry domain]
      (let [option (js/document.createElement "option")
            is-selected (= (:value entry) (:value av))]
       (.setAttribute option "value" (name (:value entry)))
       (aset option "textContent" (name (:value entry)))
       (when is-selected
         (.setAttribute option "selected" true))
       (.appendChild select option)))
    select))

(defmethod get-element-value :editor [type editor]
  (aget (:element editor) "value"))

(defmethod get-element-value :domain-editor [type editor]
  (let [select (:element editor)
        option (aget (.-options select) (.-selectedIndex select))]
    (keyword (aget option "text"))))

(defn- set-editor-style [input drawable]
  (let [style (.-style input)]
    (aset style "font-size" (str (d/getp drawable :font-size) "px"))
    (aset style "font-family" (str (d/getp drawable :font-family)))))

(defn- set-editor-pos [input drawable]
  (let [style (.-style input)]
    (aset style "position" "absolute")
    (aset style "left" (str (+ (d/get-left drawable) 5) "px"))
    (aset style "top" (str (d/get-top drawable) "px"))
    (aset style "width" (str (+ (d/get-width drawable) 15) "px"))
    (aset style "height" (str (+ (d/get-height drawable) 5) "px"))))

(defn commit [type]
  (when-let [editor (get-state type)]
    (let [value (get-element-value type editor)
          eid (-> editor :entity :uid)
          aid (-> editor :attribute-value :id)]
       (e/update-attribute-value eid aid value)
       (p/remove-state type)
       (.removeChild (p/get-container) (:element editor)))))

(defn- events [root element type]
  (.addEventListener element "change" #(commit type))
  (.addEventListener element "blur" #(commit type)))

(defn- open [event type]
  (commit)
  (let [root (p/get-container)
        drawable (:drawable event)
        av (:attribute-value event)
        element (create-element type av)
        style (.-style element)]
    (events root element type)
    (set-editor-pos element drawable)
    (append-state type element event)
    (.appendChild root element)
    (.focus element)))

(defn editor [event]
  (open event :editor))

(defn domain-editor [event]
  (open event :domain-editor))
