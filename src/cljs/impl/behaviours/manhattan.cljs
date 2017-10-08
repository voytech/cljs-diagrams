(ns impl.behaviours.manhattan
  (:require [core.entities :as e]
            [core.layouts :as layouts]
            [core.drawables :as d]
            [core.eventbus :as b]
            [impl.behaviours.standard-api :as std]
            [impl.drawables :as dimpl]
            [impl.components :as c]))

(def INSET-WIDTH 20)

(def directions {:right-down           (juxt :rm :rb)
                 :right-down-half-left (juxt :rm :rb :bm)
                 :right-down-full-left (juxt :rm :rb :lb)
                 :right-up             (juxt :rm :rt)
                 :right-up-half-left   (juxt :rm :rt :tm)
                 :right-up-full-left   (juxt :rm :rt :lt)
                 :left-down            (juxt :lm :lb)
                 :left-down-half-right (juxt :lm :lb :bm)
                 :left-down-full-right (juxt :lm :lb :rb)
                 :left-up              (juxt :lm :lt)
                 :left-up-half-right   (juxt :lm :lt :tm)
                 :left-up-full-right   (juxt :lm :lt :rt)
                 :top-left             (juxt :tm :lt)
                 :top-left-half-down   (juxt :tm :lt :lm)
                 :top-left-full-down   (juxt :tm :lt :lb)
                 :top-right            (juxt :tm :rt)
                 :top-right-half-down  (juxt :tm :rt :rm)
                 :top-right-full-down  (juxt :tm :rt :rb)
                 :bottom-left          (juxt :bm :lb)
                 :bottom-left-half-up  (juxt :bm :lb :lm)
                 :bottom-left-full-up  (juxt :bm :lb :lt)
                 :bottom-right         (juxt :bm :rb)
                 :bottom-right-half-up (juxt :bm :rb :rm)
                 :bottom-right-full-up (juxt :bm :rb :rt)})

(defn- follow-direction [dir node-points]
  ((get directions dir) node-points))

(defn- center-point [cmp]
  (let [drwbl (:drawable cmp)
        mx (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2))
        my (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2))]
    {:x mx :y my}))

(defn- control-side-2-node-point-type [control-side]
  (cond
    (= control-side :left)   :lm
    (= control-side :right)  :rm
    (= control-side :top)    :tm
    (= control-side :bottom) :bm))

(defn- eval-normals [start end]
   (if (> (- (:x end) (:x start)) (- (:y end) (:y start))) [:h :h] [:v :v]))

(defn- compute-mid-points [sp ep s-normal e-normal]
  (let [dx (- (:x ep) (:x sp))
        dy (- (:y ep) (:y sp))]
     (cond
       (and (= :h e-normal) (= :h s-normal)) [{:x (+ (:x sp) (/ dx 2)) :y (:y sp)} {:x (+ (:x sp) (/ dx 2)) :y (:y ep)}]
       (and (= :v e-normal) (= :v s-normal)) [{:x (:x sp) :y (+ (:y sp) (/ dy 2))} {:x (:x ep) :y (+ (:y sp) (/ dy 2))}]
       (and (= :v s-normal) (= :h e-normal)) [{:x (:x sp) :y (:y ep)}]
       (and (= :h s-normal) (= :v e-normal)) [{:x (:x ep) :y (:y sp)}])))

(defn- compute-node-points [node-entity-or-main-cmpnt inset-width]
  (let [main (if (instance? e/Entity node-entity-or-main-cmpnt)
               (first (e/get-entity-component node-entity-or-main-cmpnt ::c/main))
               node-entity-or-main-cmpnt)
        drwbl (:drawable main)]
   {:lt {:x (- (d/get-left drwbl) inset-width) :y (- (d/get-top drwbl) inset-width)}
    :lm {:x (- (d/get-left drwbl) inset-width) :y (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2))}
    :lb {:x (- (d/get-left drwbl) inset-width) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width)}
    :rt {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (- (d/get-top drwbl) inset-width)}
    :rm {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (+ (d/get-top drwbl) (/ (d/get-height drwbl) 2))}
    :rb {:x (+ (d/get-left drwbl) (d/get-width drwbl) inset-width) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width)}
    :tm {:x (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2)) :y (- (d/get-top drwbl) inset-width)}
    :bm {:x (+ (d/get-left drwbl) (/ (d/get-width drwbl) 2)) :y (+ (d/get-top drwbl) (d/get-height drwbl) inset-width)}}))

(defn- from-leftmost-to-tright-sleft-edge-match [trg-node-points src-node-points]
  (<  (-> trg-node-points :rm :x) (-> src-node-points  :lm :x)))

(defn- from-tright-sleft-edge-match-to-tright-in-middle [trg-node-points src-node-points]
  (and (>=  (-> trg-node-points :rm :x) (-> src-node-points  :lm :x))
       (< (-> trg-node-points :rm :x) (-> src-node-points :tm :x))))

(defn- from-tright-in-middle-to-tleft-in-middle  [trg-node-points src-node-points]
  (and (>=  (-> trg-node-points :rm :x) (-> src-node-points  :tm :x))
       (< (-> trg-node-points :tm :x) (-> src-node-points :rm :x))))

(defn- from-tleft-in-middle-to-tleft-sright-edge-match  [trg-node-points src-node-points]
  (and (>=  (-> trg-node-points :tm :x) (-> src-node-points  :rm :x))
       (<= (-> trg-node-points :lm :x) (-> src-node-points :rm :x))))

(defn- from-tleft-sright-edge-match-to-rightmost  [trg-node-points src-node-points]
  (> (-> trg-node-points :lm :x) (-> src-node-points :rm :x)))

(defn- from-topmost-to-tbottom-stop-edge-match [trg-node-points src-node-points]
  (<  (-> trg-node-points :bm :y) (-> src-node-points  :tm :y)))

(defn- from-tbottom-stop-edge-match-to-tbottom-in-middle   [trg-node-points src-node-points]
  (and (>=  (-> trg-node-points :bm :y) (-> src-node-points  :tm :y))
       (< (-> trg-node-points :bm :y) (-> src-node-points :lm :y))))

(defn- from-tbottom-in-middle-to-ttop-in-middle  [trg-node-points src-node-points]
  (and (>= (-> trg-node-points :bm :y) (-> src-node-points :lm :y))
       (< (-> trg-node-points :lm :y) (-> src-node-points :bm :y))))

(defn- from-ttop-in-middle-to-ttop-sbottom-edge-match  [trg-node-points src-node-points]
  (and (>= (-> trg-node-points :lm :y) (-> src-node-points :bm :y))
       (<= (-> trg-node-points :tm :y) (-> src-node-points :bm :y))))

(defn- from-ttop-sbottom-edge-match-to-bottommost  [trg-node-points src-node-points]
  (> (-> trg-node-points :tm :y) (-> src-node-points :bm :y)))

(defn- top-right-path [src-node-points trg-node-points]
  (cond
    (from-leftmost-to-tright-sleft-edge-match trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg [(:rm trg-node-points)]}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-left src-node-points) :trg [(:rm trg-node-points)]}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-left src-node-points) :trg [(:rm trg-node-points)]}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-left-half-down src-node-points) :trg [(:rm trg-node-points)]}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-left-full-down src-node-points) :trg [(:rm trg-node-points)]})
    (from-tright-sleft-edge-match-to-tright-in-middle trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg [(:rm trg-node-points)]}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg [(:rm trg-node-points)]}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg [(:rm trg-node-points)]}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-left-full-down src-node-points) :trg (follow-direction :right-up trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-left-full-down src-node-points) :trg (follow-direction :right-up trg-node-points)})
    (from-tright-in-middle-to-tleft-in-middle trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src  [(:tm src-node-points)] :trg (follow-direction :right-down trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src  [(:tm src-node-points)] :trg (follow-direction :right-down trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src  [(:tm src-node-points)] :trg (follow-direction :right-up trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src  (follow-direction :top-right-full-down src-node-points) :trg [(:rm trg-node-points)]}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-right-full-down src-node-points) :trg (follow-direction :right-up trg-node-points)})
    (from-tleft-in-middle-to-tleft-sright-edge-match trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :right-down-half-left trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :right-down-half-left trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :right-up-half-left trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-right-half-down src-node-points) :trg (follow-direction :right-up-half-left trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-right-full-down src-node-points) :trg (follow-direction :right-up-half-left trg-node-points)})
    (from-tleft-sright-edge-match-to-rightmost trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :right-down-full-left trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :right-up-full-left trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :right-up-full-left trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :right-up-full-left trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-right-full-down src-node-points) :trg (follow-direction :right-up-full-left trg-node-points)})))

(defn- top-top-path [src-node-points trg-node-points]
  (cond
    (from-leftmost-to-tright-sleft-edge-match trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-left src-node-points) :trg (follow-direction :top-right trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-left src-node-points) :trg (follow-direction :top-right trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-left src-node-points) :trg (follow-direction :top-right trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-left-half-down src-node-points) :trg (follow-direction :top-right trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-left-full-down src-node-points) :trg (follow-direction :top-right trg-node-points)})
    (from-tright-sleft-edge-match-to-tright-in-middle trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg (follow-direction :top-right trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg (follow-direction :top-right trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg (follow-direction :top-right trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-left src-node-points) :trg (follow-direction :top-right trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-left src-node-points) :trg (follow-direction :top-right trg-node-points)})
    (from-tright-in-middle-to-tleft-in-middle trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src  [(:tm src-node-points)] :trg (follow-direction :top-right-full-down trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src  [(:tm src-node-points)] :trg (follow-direction :top-right-full-down trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src  [(:tm src-node-points)] :trg (follow-direction :top-right-half-down trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src  (follow-direction :top-right src-node-points) :trg [(:tm trg-node-points)]}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-right-full-down src-node-points) :trg (follow-direction :top-right trg-node-points)})
    (from-tleft-in-middle-to-tleft-sright-edge-match trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg (follow-direction :top-right-full-down trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg (follow-direction :top-left-half-down trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src [(:tm src-node-points)] :trg (follow-direction :top-left trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-right-half-down src-node-points) :trg (follow-direction :top-left trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-right-full-down src-node-points) :trg (follow-direction :top-left trg-node-points)})
    (from-tleft-sright-edge-match-to-rightmost trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :top-left trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :top-left trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :top-left trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :top-right src-node-points) :trg (follow-direction :top-left trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :top-right-full-down src-node-points) :trg (follow-direction :top-left trg-node-points)})))

(defn- bottom-bottom-path [src-node-points trg-node-points]
  (cond
    (from-leftmost-to-tright-sleft-edge-match trg-node-points src-node-points)
    (cond
       (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
       {:src (follow-direction :bottom-left src-node-points) :trg (follow-direction :bottom-right trg-node-points)}
       (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
       {:src (follow-direction :bottom-left src-node-points) :trg (follow-direction :bottom-right trg-node-points)}
       (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
       {:src (follow-direction :bottom-left src-node-points) :trg (follow-direction :bottom-right trg-node-points)}
       (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
       {:src (follow-direction :bottom-left src-node-points) :trg (follow-direction :bottom-right trg-node-points)}
       (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
       {:src (follow-direction :bottom-left src-node-points) :trg (follow-direction :bottom-right trg-node-points)})
    (from-tright-sleft-edge-match-to-tright-in-middle trg-node-points src-node-points)
    (cond
      (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
      {:src (follow-direction :bottom-left src-node-points) :trg [(:bm trg-node-points)]}
      (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
      {:src (follow-direction :bottom-left src-node-points) :trg [(:bm trg-node-points)]}
      (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
      {:src [(:bm src-node-points)] :trg (follow-direction :bottom-right trg-node-points)}
      (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
      {:src [(:bm src-node-points)] :trg (follow-direction :bottom-right trg-node-points)}
      (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
      {:src [(:bm src-node-points)] :trg (follow-direction :bottom-right trg-node-points)})
    (from-tright-in-middle-to-tleft-in-middle trg-node-points src-node-points)
    (cond
      (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
      {:src (follow-direction :bottom-right-full-up src-node-points) :trg [(:bm trg-node-points)]}
      (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
      {:src [(:bm src-node-points)] :trg  [(:bm trg-node-points)]}
      (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
      {:src [(:bm src-node-points)] :trg  [(:bm trg-node-points)]}
      (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
      {:src [(:bm src-node-points)] :trg (follow-direction :bottom-right-full-up trg-node-points)}
      (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
      {:src [(:bm src-node-points)] :trg (follow-direction :bottom-right-full-up trg-node-points)})
    (from-tleft-in-middle-to-tleft-sright-edge-match trg-node-points src-node-points)
    (cond
      (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg [(:bm trg-node-points)]}
      (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg  [(:bm trg-node-points)]}
      (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg (follow-direction :bottom-left trg-node-points)}
      (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg (follow-direction :bottom-left trg-node-points)}
      (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg (follow-direction :bottom-left trg-node-points)})
    (from-tleft-sright-edge-match-to-rightmost trg-node-points src-node-points)
    (cond
      (from-topmost-to-tbottom-stop-edge-match trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg  (follow-direction :bottom-left trg-node-points)}
      (from-tbottom-stop-edge-match-to-tbottom-in-middle trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg  (follow-direction :bottom-left trg-node-points)}
      (from-tbottom-in-middle-to-ttop-in-middle trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg (follow-direction :bottom-left trg-node-points)}
      (from-ttop-in-middle-to-ttop-sbottom-edge-match trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg (follow-direction :bottom-left trg-node-points)}
      (from-ttop-sbottom-edge-match-to-bottommost trg-node-points src-node-points)
      {:src (follow-direction :bottom-right src-node-points) :trg (follow-direction :bottom-left trg-node-points)})))

(defn node-path-begining [source-node-main-cmpnt source-control-side target-node-main-cmpnt target-control-side]
  (let [src-node-points (compute-node-points source-node-main-cmpnt INSET-WIDTH)
        trg-node-points (compute-node-points target-node-main-cmpnt INSET-WIDTH)]
    (cond
       (and (= :top source-control-side) (= :right target-control-side))
       (assoc (top-right-path src-node-points trg-node-points) :reversed false)
       (and (= :right source-control-side) (= :top target-control-side))
       (assoc (top-right-path trg-node-points src-node-points) :reversed true)
       (and (= :top source-control-side) (= :top target-control-side))
       (assoc (top-top-path src-node-points trg-node-points) :reversed false)
       (and (= :bottom source-control-side) (= :bottom target-control-side))
       (assoc (bottom-bottom-path src-node-points trg-node-points) :reversed false))))

(defn- compute-candidate-points [entity start end s-normal e-normal]
  (let [sp (center-point start)
        ep (center-point end)
        source-node-id (e/component-property entity (:name start) :rel-entity-uid)
        target-node-id (e/component-property entity (:name end) :rel-entity-uid)]
    (if-not (or (nil? source-node-id) (nil? target-node-id))
      (let [source-c-side (e/component-property entity (:name start) :rel-connector)
            target-c-side (e/component-property entity (:name end) :rel-connector)
            source-node (e/entity-by-id source-node-id)
            target-node (e/entity-by-id target-node-id)
            path-beginings (node-path-begining source-node source-c-side target-node target-c-side)
            src-path-begin (:src path-beginings)
            trg-path-begin (:trg path-beginings)
            reversed (:reversed path-beginings)
            src-path-begin-point (last src-path-begin)
            trg-path-begin-point (last trg-path-begin)
            normals (eval-normals src-path-begin-point trg-path-begin-point)
            mid-points (compute-mid-points src-path-begin-point trg-path-begin-point (normals 0) (normals 1))]
        (if reversed (reverse (concat src-path-begin mid-points (rseq trg-path-begin)))
                     (concat src-path-begin mid-points (rseq trg-path-begin))))
      (compute-mid-points (center-point start) (center-point end) s-normal e-normal))))

(defn- compute-path [start-point end-point mid-points]
  (let [all-points (flatten [start-point mid-points end-point])]
     (partition 2 1 all-points)))

(defn- update-line-component [entity idx sx sy ex ey]
  (e/assert-component entity (str "line-" idx) ::c/relation {:x1 sx :y1 sy :x2 ex :y2 ey}))

(defn- update-line-components [entity path]
  (let [remove-components (filter (fn [c]
                                    (let [splt (clojure.string/split (:name c) #"-")]
                                      (when (> (count splt) 1)
                                        (>= (cljs.reader/read-string (splt 1)) (count path))))) (e/components-of entity))]
    (if (> (count remove-components) 0)
      (doseq [component remove-components] (e/remove-entity-component entity (:name component))))
    (-> (map-indexed (fn [idx e]
                         (update-line-component entity idx (:x (first e))
                                                           (:y (first e))
                                                           (:x (last e))
                                                           (:y (last e)))) path)
        last)))

(defn update-manhattan-layout [entity s-normal e-normal]
  (let [start (e/get-entity-component entity "start")
        end (e/get-entity-component entity "end")
        mid-points (compute-candidate-points entity start end s-normal e-normal)
        path (compute-path (center-point start) (center-point end) mid-points)]
     (-> (update-line-components entity path)
         (std/refresh-arrow-angle (e/get-entity-component entity "arrow")))))

(defn calculate-normals [entity startpoint endpoint]
  (let [start-c-point (center-point startpoint)
        end-c-point (center-point endpoint)
        startpoint-connector (e/component-property entity (:name startpoint) :rel-connector)
        endpoint-connector (e/component-property entity (:name endpoint) :rel-connector)]
    (cond
       (and (= :right startpoint-connector)  (= :left endpoint-connector)) [:h :h]
       (and (= :top startpoint-connector)    (= :left endpoint-connector)) [:v :h]
       (and (= :bottom startpoint-connector) (= :left endpoint-connector)) [:v :h]
       :else (if (> (- (:x end-c-point) (:x start-c-point)) (- (:y end-c-point) (:y start-c-point))) [:h :h] [:v :v]))))

(defn clear-orphan-components [entity start end]
  (e/remove-entity-component entity "connector"))

(defn do-manhattan-layout []
  (fn [e]
     (let [endpoint (:component e)
           entity   (:entity e)
           drawable (:drawable endpoint)
           start (e/get-entity-component entity "start")
           end (e/get-entity-component entity "end")
           connector (e/get-entity-component entity "connector")
           normals (calculate-normals entity start end)]
        (clear-orphan-components entity start end)
        (cond
          (= ::c/startpoint (:type endpoint)) (std/position-startpoint entity (:movement-x e) (:movement-y e) :offset true)
          (= ::c/endpoint   (:type endpoint)) (std/position-endpoint   entity (:movement-x e) (:movement-y e) :offset true))
        (update-manhattan-layout entity (normals 0) (normals 1)))))
