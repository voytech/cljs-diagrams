# Cljs-Relational-Designer

Clojurescript library/framework for developing different kinds of diagrams.  
Key functionalities:
- Support for different kind of entities (diagram entity classes) via DSL 
- Creating relationships between entities
- Behaviours - application events model based of native DOM events (via core.async and simple event-bus)
- Auto-wiring of behaviours - inferring (and bus registration) of behaviours from entity specification (e.g. types of managed components) (Subject to change)
- Support for custom rendering engines 
- SVG renderer engine
- Canvas renderer engine (using fabric.js)
- Pluggable architecture via event bus and loose coupling.
- First class building blocks represented by macros: defentity, defcomponent, defbehaviour, deflayout and so on

## Dependencies

- java 1.8+
- [boot][1]

## Architecture 



## Usage
Cljs-Relation-Designer is shipped with number of display components acting as building blocks for new entities.
When components are composed within entity they can activate different kind of entity behaviours depending on component types.
E.g.: 
- when one adds 'control' components into entity, entity will automatically become resizable and connectible with other entities.
- when one adds 'text' component, text will automatically change its border colour on hover.

Of course there is no limitation to use only existing set of components. You can always define new components as follows:
 
```clojure
(defcomponent relation :draw-line {} (relation-initializer))
```

where:

'relation' - is a component function name (a component simply groups display properties for shape and gives it a namespace qualified name) 

':draw-line' - is a rendering method key. Many shapes with similar display properties may be rendered differently. E.g. clircle may have following properties: left, top, width, height. On the other hand, rectangle may have the same set of properties too. Rendering method helps to choose right rendering logic for particular component type. 

'relation-initializer' - is function as follows: 

```clojure
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
```
'realtion-initializer' is a lazy display properties generator. First argument of realtion-initializer is an owning entity. Reason why relation-initializer is a lazy properties evaluator should be obvious. Sometimes it may be required to know some properties of entities in order to correctly display specific component. 

Component means nothing without an entity, and it can only be displayed after being attached as an entity child.

This is how entity definition looks like: 

```clojure
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
```

And this is how to render this entity on DOM element: 

```clojure
(association app-state options)
```

where app-state is an application state ( will be explained ) and options are usually coordinates {:left ... :top ...}  relative to enclosing DOM element.

## Screenshots

## License

Copyright © 2018, **Wojciech Maka**

[1]: https://github.com/tailrecursion/boot
[2]: https://github.com/technomancy/leiningen
[3]: http://localhost:8000
