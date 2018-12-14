# Cljs-Relational-Designer

Clojurescript library/framework for developing different kinds of diagrams.  
Key functionalities:
- Support for different kind of entities (diagram entity classes) via DSL 
- Creating relationships between entities
- Behaviours - native event handling leveraged to application events model (via core.async and simple event-bus)
- Auto-wiring of behaviours (Subject to change)
- Support for custom rendering engines
- SVG renderer engine
- Canvas renderer engine (using fabric.js)
- Pluggable architecture via event bus and loose coupling of modules.
- First class building blocks represeted using macros: defentity, defcomponent, defbehaviour, deflayout and so on

## Dependencies

- java 1.8+
- [boot][1]

## Architecture 



## Usage
Cljs-Relation-Designer has bunch of components already present for consumers:

e.g.
(defcomponent relation :draw-line {} (relation-initializer))

where:
relation-initializer is function as follows: 

(defn- relation-initializer []
 (fn [container props]
   {:x1  0
    :y1  0
    :x2 (-> container :size :width)
    :y2 0
    :left 0
    :top 0
    :border-color "black"
    :border-style :solid
    :border-width 1
    :z-index :before-bottom}))

'relation' is a component, a component simply groups display properties for shape and gives it a namespace qualified name. 
'realtion-initializer' is a display property map generator for late evaluation, after component becomes child of an entity. First argument of realtion-initializer is a component owning container - an entity.  

Component means nothing without an entity, and it can only be displayed after being attached as an entity child.

Lets see how entity definition looks like: 

(defentity association
  {:width 180 :height 150}
  (with-layouts
    (layout :attributes l/default-flow-layout (cl/having-layout-property :attributes) relation-layout-options))
  (components-templates
    (component-template ::c/relation {:border-width 3}))
  (with-components data options
    (component c/relation "connector" {} {})
    (component c/startpoint "start" {} {})
    (component c/arrow "arrow" {} {})
    (component c/endpoint "end" {} {})
    (component c/title "title" {:text "Title."} {:layout :attributes})))


## Screenshots

## License

Copyright © 2018, **Wojciech Maka**

[1]: https://github.com/tailrecursion/boot
[2]: https://github.com/technomancy/leiningen
[3]: http://localhost:8000
