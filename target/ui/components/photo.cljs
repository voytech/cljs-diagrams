(ns ui.components.photo (:require [utils.dom.dnd-utils :as dnd] [utils.dom.dom-utils :as dom] [tailrecursion.hoplon :refer [form audio input hgroup do! timeout $text rely base h1 embed h3 body keygen val-id on-append! progress main cite object i p nav ruby relx check-val! a menu blockquote img $comment span track seq?* data u dl select html thead del eventsource append-child fieldset rel aside figure figcaption q on! bdi video address caption parse-args by-id dd rp hr tbody table acronym frame applet html-var add-initfn! pre ul dir html-time add-attributes! html-map sup dfn sub mark script big button wbr strong li dt frameset td tr section th optgroup rel-event iframe legend em kbd spliced article isindex abbr command source output basefont route-cell header datalist tfoot s ins footer title is-ie8 h5 canvas param font div option summary samp center small style textarea loop-tpl* strike h4 tt head add-children! ol details col vector?* label rt when-dom h6 link colgroup meter html-meta text-val! bdo b code node? noframes replace-children! noscript safe-nth h2 area br unsplice]] [tailrecursion.javelin :refer [input? cell cell? destroy-cell! ^{:private true} last-rank ^{:deprecated true} lift lens? set-formula! cell-doseq* ^{:private true, :dynamic true} *tx* deref* set-cell! lens formula? alts! dosync* cell-map formula]]) (:require-macros [tailrecursion.hoplon :refer [text with-timeout sexp defelem def-values loop-tpl with-interval with-init!]] [tailrecursion.javelin :refer [with-let mx2 dosync cell= set-cell!= prop-cell cell-doseq defc cell-let-1 defc= macroexpand-all mx cell-let]]))

(defn dragover [event] (.log js/window.console event) (.stopPropagation event) (.preventDefault event))

(declare photo-view)

(declare select-files!)

(defn load-file [file callback] (let [reader (js/FileReader.)] (set! (.-onload reader) (fn* [p1__2145#] (callback {:name (.-name file), :type (.-type file), :content (.-result (.-target p1__2145#))}))) (.readAsDataURL reader file)))

(defn drop! [event handle] (.stopPropagation event) (.preventDefault event) (dnd/data-transfer event))

(defn select-files! [event callback] (let [files (.-files (.-target event))] (let [file (aget files 0)] (load-file file callback))))

(defelem photo-loader [{:keys [class callback]}] (div :class class :id "drop-area" :drop (fn* [p1__2146#] (drop! p1__2146# callback)) :on-dragover dragover (div :class "file-load-wrapper" (input :id "fileload" :type "file" :name "files[]" :class "filestyle" :change (fn* [p1__2147#] (select-files! p1__2147# callback))))))

(defelem photo-list [{:keys [list]}] (div) (div :class "thumbs-container" (loop-tpl :bindings [{:keys [name content]} list] (photo-view :name name :content content))))

(defelem photo-view [{:keys [name content]}] (div :class "photo-thumbnail" (img :class "photo" :id name :src content :draggable "true" :dragstart (fn* [p1__2148#] (dnd/set-dnd-data p1__2148# "imgid" (clojure.core/deref name) "move")))))
