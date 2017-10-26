(ns core.model)

(def drawable-properties {:left {:desc "position of left corner on x axis" :for #{}}
                          :top  {:desc "position of top corner on y axis" :for #{}}
                          :img  {:desc "image url or image base64 data, or other image representation" :for #{}}
                          :text {:desc "a text to be rendered" :for #{}}
                          :width  {:desc "width of the component" :for #{}}
                          :height {:desc "height of the component" :for #{}}
                          :origin-x {:desc "x axis alligned origin of the component" :for #{}}
                          :origin-y {:desc "y axis alligned origin of the component" :for #{}}
                          :angle {:desc "angle by which a component should be rotated" :for #{}}
                          :x1 {:desc "valid for line drawables - starting point x coordinate" :for #{:connector :line}}
                          :y1 {:desc "valid for line drawables - starting point y coordinate" :for #{:connector :line}}
                          :x2 {:desc "valid for line drawables - terminating point x coordinate" :for #{:connector :line}}
                          :y2 {:desc "valid for line drawables - terminating point y coordinate" :for #{:connector :line}}
                          :z-index {:desc "directives telling how to layer component" :for #{}}
                          :border-color  {:desc "color of border line" :for #{}}
                          :background-color {:desc "background color of component" :for #{}}
                          :radius {:desc "radius of a circle component" :for #{:circle}}
                          :font-family {:desc "font family of text component" :for #{:text}}
                          :font-weight {:desc "font weight of text component" :for #{:text}}
                          :font-size {:desc "font size of text component" :for #{:text}}
                          :text-align {:desc "text alignment of component" :for #{:text}}
                          :visible {:desc "if component should be visible" :for #{}}
                          :border-width {:desc "the width of component border" :for #{}}
                          :opacity (:desc "opacity of the component" :for #{})
                          :round-corners (:desc "value by which to round corners" :for #{})
                          :color {:desc "forecolour of component" :for #{}}})

(defn is-valid-property [component property]
 (let [tags (set (concat [:type component] :tags component))]
   (when-let [prop-def (get drawable-properties property)]
      (or (= 0 (count (:dor prop-def))) (> (count (intersection (:for prop-def) tags)) 0)))))

(defn canonical-event-names {".move"   {}
                             "component.hover"  {}
                             "component.out"    {}
                             "component.select" {}
                             "component.enter"  {}
                             "component.unselect" {}
                             "component.create" {}
                             "component.delete" {}
                             "entity.move" {}
                             "entity.select" {}
                             "entity.unselect" {}
                             "entity.create" {}
                             "entity.delete" {}})
