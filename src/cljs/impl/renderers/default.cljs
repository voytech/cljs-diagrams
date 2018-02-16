(ns impl.renderers.default
  (:require [core.utils.general :refer [make-js-property]]
            [core.components :as d]
            [core.eventbus :as b]
            [core.rendering :as r]
            [impl.components :as impld]))

;Some fabric object specific options to pass.
(def RESTRICTED_BEHAVIOUR {:hasRotatingPoint false
                           :lockRotation true
                           :lockScalingX true
                           :lockScalingY true})
(def LOCKED_SCALING       {:lockScalingX true
                           :lockScalingY true})
(def LOCKED_ROTATION      {:lockRotation true})
(def LOCKED_MOVEMENT      {:lockMovementX true
                           :lockMovementY true})
(def NO_DEFAULT_CONTROLS  {:hasControls false
                           :hasBorders false
                           :hasRotatingPoint false})

(def LOCKED (merge LOCKED_MOVEMENT LOCKED_ROTATION LOCKED_SCALING NO_DEFAULT_CONTROLS))

(defn- fabric-set
  ([source property value]
   (.set source property value))
  ([source map_]
   (.set source (clj->js map_))))


(defonce fabric-property-mapping {:left "left"
                                  :top  "top"
                                  :text "text"
                                  :width  "width"
                                  :height "height"
                                  :origin-x "originX"
                                  :origin-y "originY"
                                  :angle "angle"
                                  :x1 "x1"
                                  :y1 "y1"
                                  :x2 "x2"
                                  :y2 "y2"
                                  :z-index "zIndex"
                                  :border-color  "stroke"
                                  :background-color "fill"
                                  :radius "radius"
                                  :font-family "fontFamily"
                                  :font-weight "fontWeight"
                                  :font-size "fontSize"
                                  :text-align "textAlign"
                                  :visible "visible"
                                  :color "color"
                                  :border-width "strokeWidth"})

(defn- to-fabric-property-map [input-map]
  (apply merge (mapv (fn [e] {(keyword (or (e fabric-property-mapping) e)) (e input-map)}) (keys input-map))))

(defn- fabric-apply [drawable source properties]
  (doseq [p properties]
    (fabric-set source (or (p fabric-property-mapping) p) (d/getp drawable p))))

(defn- synchronize-bounds [drawable]
  (let [source (:data (r/get-state-of drawable))]
    (when (or (nil? (d/get-width drawable)) (= :text (:type drawable))) (d/set-width  drawable (.-width  source)))
    (when (or (nil? (d/get-height drawable)) (= :text (:type drawable))) (d/set-height drawable (.-height source)))))

(defn- property-change-render [drawable rendering-context]
  (let [source  (:data (r/get-state-of drawable))
        redraw   (get-in rendering-context [:redraw-properties (:uid drawable)])]
      (fabric-apply drawable source redraw)
      (synchronize-bounds drawable)))

(defn- fabric-create-rendering-state [context drawable create]
  (let [fabric-object (create)]
     (make-js-property fabric-object "refId" (:uid drawable))
     (.add (:canvas context) fabric-object)
     {:data fabric-object}))

; in drawable-state we holds an fabric.js object.
; in context we have serveral properties. One of them is fabric.js canvas reference.
(defn- fabric-destroy-rendering-state [context state]
  (let [canvas (:canvas context)]
    (.remove canvas (:data state))))

(defmethod r/all-rendered :fabric [context]
  (.renderAll (get context :canvas)))

;;==========================================================================================================
;; rect rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric ::impld/main :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/main :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Rect. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/main :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

(defmethod r/do-render [:fabric ::impld/control :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/control :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Rect. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/control :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; startpoint rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric ::impld/startpoint :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/startpoint :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/startpoint :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; endpoint rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric ::impld/endpoint :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/endpoint :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/endpoint :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; breakpoint rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric ::impld/breakpoint :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/breakpoint :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Circle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/breakpoint :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; line rendering (must be improved ... changes to x1 x2 y1 y2 on static canvas not working as expected)
;; As a temporary workaround - we are removing rendering state of drawable and creating new line as a new rendering state with new
;; x1 x2 y1 y2 properties. Also take into account that synchronization of x1 x2 y1 y2 with left top widht height must be better.
;;==========================================================================================================
(defmethod r/do-render [:fabric ::impld/relation :default] [drawable rendering-context]
  ;(fabric-destroy-rendering-state rendering-context (r/get-state-of drawable))
  ;(let [data (dissoc (to-fabric-property-map (d/model drawable)) :width :height :left :top)
  ;      state (fabric-create-rendering-state rendering-context drawable (fn [] (js/fabric.Line. (clj->js [(:x1 data) (:y1 data) (:x2 data) (:y2 data)]) (clj->js data))))
  ;  (.moveTo (:data state) (get data "zIndex"))
  ;  (r/update-state drawable state)
  (property-change-render drawable rendering-context))

(defmethod r/create-rendering-state [:fabric ::impld/relation :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
    (fabric-create-rendering-state context drawable (fn [] (js/fabric.Line. (clj->js [(:x1 data) (:y1 data) (:x2 data) (:y2 data)]) (clj->js data))))))


(defmethod r/destroy-rendering-state [:fabric ::impld/relation :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; triangle rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric ::impld/arrow :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/arrow :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Triangle. (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/arrow :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

;;==========================================================================================================
;; text rendering
;;==========================================================================================================
(defmethod r/do-render [:fabric ::impld/value :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/value :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Text. (:text data) (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/value :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))

(defmethod r/do-render [:fabric ::impld/description :default] [drawable context]
  (property-change-render drawable context))

(defmethod r/create-rendering-state [:fabric ::impld/description :default] [drawable context]
  (let [data (to-fabric-property-map (d/model drawable))]
   (fabric-create-rendering-state context drawable (fn [] (js/fabric.Text. (:text data) (clj->js data))))))

(defmethod r/destroy-rendering-state [:fabric ::impld/description :default] [drawable context]
  (fabric-destroy-rendering-state context (r/get-state-of drawable)))
