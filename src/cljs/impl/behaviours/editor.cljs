(ns impl.behaviours.editor
 (:require [core.entities :as e]
           [impl.behaviours.standard-api :as behaviours]
           [core.project :as p]
           [impl.drawables :as di]
           [core.drawables :as d]
           [impl.components :as c]
           [core.behaviours :as b]
           [core.events :as ev]
           [core.options :as o]))

(defn- set-editor-style [input drawable]
  (let [style (.-style input)]
    (aset style "font-size" (str (d/getp drawable :font-size) "px"))
    (aset style "font-family" (str (d/getp drawable :font-family)))))

(defn- set-editor-pos [input drawable]
  (let [style (.-style input)]
    (aset style "position" "absolute")
    (aset style "left" (str (d/get-left drawable) "px"))
    (aset style "top" (str (d/get-top drawable) "px"))
    (aset style "width" (str (+ (d/get-width drawable) 25) "px"))
    (aset style "height" (str (+ (d/get-height drawable) 5) "px"))))

(defn commit []
  (when-let [editor (p/get-state :editor)]
    (let [value (aget (:input editor) "value")
          eid (-> editor :entity :uid)
          aid (-> editor :attribute-value :id)]
       (e/set-attribute-value eid aid value)
       (.removeChild (p/get-container) (:input editor)))))

(defn open [event]
  (let [root (p/get-container)
        drawable (:drawable event)
        input (js/document.createElement "input")
        style (.-style input)]
    (.addEventListener input "change" commit)
    (set-editor-pos input drawable)
    (aset input "value" (e/get-attribute-value-data (:attribute-value event)))
    (p/append-state :editor {:input input
                             :entity (:entity event)
                             :attribute-value (:attribute-value event)})
    (.appendChild root input)))
