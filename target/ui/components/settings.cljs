(ns ui.components.settings (:require [core.canvas-interface :as interface] [core.settings :as settings] [tailrecursion.hoplon :refer [form audio input hgroup do! timeout $text rely base h1 embed h3 body keygen val-id on-append! progress main cite object i p nav ruby relx check-val! a menu blockquote img $comment span track seq?* data u dl select html thead del eventsource append-child fieldset rel aside figure figcaption q on! bdi video address caption parse-args by-id dd rp hr tbody table acronym frame applet html-var add-initfn! pre ul dir html-time add-attributes! html-map sup dfn sub mark script big button wbr strong li dt frameset td tr section th optgroup rel-event iframe legend em kbd spliced article isindex abbr command source output basefont route-cell header datalist tfoot s ins footer title is-ie8 h5 canvas param font div option summary samp center small style textarea loop-tpl* strike h4 tt head add-children! ol details col vector?* label rt when-dom h6 link colgroup meter html-meta text-val! bdo b code node? noframes replace-children! noscript safe-nth h2 area br unsplice]] [tailrecursion.javelin :refer [input? cell cell? destroy-cell! ^{:private true} last-rank ^{:deprecated true} lift lens? set-formula! cell-doseq* ^{:private true, :dynamic true} *tx* deref* set-cell! lens formula? alts! dosync* cell-map formula]]) (:require-macros [tailrecursion.hoplon :refer [text with-timeout sexp defelem def-values loop-tpl with-interval with-init!]] [tailrecursion.javelin :refer [with-let mx2 dosync cell= set-cell!= prop-cell cell-doseq defc cell-let-1 defc= macroexpand-all mx cell-let]]))

(defn is-snapping [key] (key (:snapping (clojure.core/deref settings/settings))))

(defn neg-snapping [key] (not (is-snapping key)))

(def zoom-label (settings/settings? :zoom))

(def attract-val (settings/settings? :snapping :attract))

(def interval-val (settings/settings? :snapping :interval))

(def pages-count (settings/settings? :pages :count))

(def multi-page? (settings/settings? :multi-page))

(def two-sided? (settings/settings? :pages :two-sided))

(def page-format (settings/settings? :page-format))

(def snapping-enabled (settings/settings? :snapping :enabled))

(def snapping-visible (settings/settings? :snapping :visible))

(defn css-visible [path] (cell= (if (true? (get-in settings/settings path)) "display:block" "display:none")))

(defelem select-input [{:keys [change value values]}] (select :change change :value value (loop-tpl :bindings [val values] (option :value val val))))

(defelem settings [] (div :id "settings-wrapper" :class "settings" (div :class "setting" (label (input :type "checkbox" :click (fn* [] (do (settings/settings! (neg-snapping :enabled) :snapping :enabled) true)) :value (settings/settings? :snapping :enabled)) " Enable snap grid")) (div :class "setting" (label (input :type "checkbox" :click (fn* [] (do (settings/settings! (neg-snapping :visible) :snapping :visible) true)) :value (settings/settings? :snapping :visible)) " Display snap grid")) (div :class "setting" (label (input :type "checkbox" :click (fn* [] (do (settings/settings! (not (:multi-page (clojure.core/deref settings/settings))) :multi-page) true)) :value (settings/settings? :multi-page)) " Multi page")) (div :class "setting" :style (css-visible [:snapping :enabled]) (label (input :type "range" :min 0 :max 100 :step 10 :value (settings/settings? :snapping :interval) :input (fn* [p1__1964#] (settings/settings! (clojure.core/deref p1__1964#) :snapping :interval))) (text "~{interval-val}") " Snap grid interval")) (div :class "setting" :style (css-visible [:snapping :enabled]) (label (input :type "range" :min 0 :max 20 :step 1 :value (settings/settings? :snapping :attract) :input (fn* [p1__1965#] (settings/settings! (clojure.core/deref p1__1965#) :snapping :attract))) (text "~{attract-val}") " Snapping attraction")) (div :class "setting" (label (input :type "range" :min 0 :max 20 :step 1 :value (settings/settings? :zoom) :input (fn* [p1__1966#] (settings/settings! (clojure.core/deref p1__1966#) :zoom))) (text "~{zoom-label}") " Zoom value")) (div :style (css-visible [:multi-page]) :class "setting" (label (input :type "range" :min 1 :max 20 :step 1 :value (settings/settings? :pages :count) :input (fn* [p1__1967#] (settings/settings! (clojure.core/deref p1__1967#) :pages :count))) (text "~{pages-count}") " Pages count")) (div :class "setting" (label) (select :change (fn* [p1__1968#] (settings/settings! (clojure.core/deref p1__1968#) :page-format)) :value (settings/settings? :page-format) (option :value "T" "Test format") (option :value "A3" "A3") (option :value "A4" "A4") (option :value "A5" "A5") (option :value "A6" "A6")) (p (text "Page format:<~{page-format}>"))) (div :id "settings-debug" :class "setting" (p (text "page width  ~{settings/page-width}")) (p (text "page height ~{settings/page-height}")) (p (text "multi page  ~{multi-page?}")) (p (text "snapping enabled ~{snapping-enabled}")) (p (text "snapping visible ~{snapping-visible}")))))
