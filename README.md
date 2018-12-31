# Cljs-Relational-Designer

Clojurescript library/framework for developing different kinds of diagrams.  
Key functionalities:
- Large library with wide range of reusable shapes (this is a joke!)
- Support for custom diagram shapes
- Defining shapes using DSL based on macro system
- Creating various relationships between entities (not only association links but for e.g. aggregation or composition or whathever)
- Behaviours abstraction - various event handlers (on steroids) with automatic source shape recognition via shape features preconditions, 
- Auto-wiring of behaviours - inferring (and bus registration) of behaviours from entity specification (e.g. types of managed components) (Subject to change)
- Support for custom rendering engines 
- SVG renderer engine - for reagent
- SVG renderer engine - for vanilla js (todo)
- Canvas renderer engine (using fabric.js) (to refactor)
- Pluggable architecture via event bus
- Quite advanced shape layout system
- Higher level, application data maps resolvers - A way to apply state known to upper layer, so it can be rendered on shape. E.g.: Assume we have to-do shape somewhere on canvas. When user adds todo item to list managed by his application - by using data-resolver - a todo list may be properly updated and rendered onto shape, without using component and shape api directly. 

## Dependencies

- java 1.8+
- [boot][1]


## Usage
Cljs-Relation-Designer is shipped with number of display components acting as building blocks for new shapes.
Types of components belonging to particular shape are determining shape features and behaviours.
E.g.: 
- when one adds 'control' components to shape, shape will automatically become resizable and connectible with other shapes.
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
'realtion-initializer' is a lazy property evaluator. First argument of realtion-initializer is an owning entity. Sometimes it may be required to know some properties of entities in order to correctly display specific component. 

Component means nothing without shape, and it can only be displayed after being attached as an shape child.

This is how shape definition looks like: 

```clojure
(defentity basic-rect
  {:width 180 :height 150}
  (with-layouts (layout ::w/weighted w/weighted-layout))
  (with-components context
    (component c/entity-shape "body" {:round-x 5 :round-y 5} {}
      (layout-hints (match-parent-position) (match-parent-size) (weighted-origin 0 0)) ::w/weighted)
    (component c/title "title" {:text "Object with header"} {}
      (layout-hints (weighted-position 0.5 0.1) (weighted-origin 0.5 0)) ::w/weighted)
    (component c/entity-controls)))
```

And this is how to render this entity on DOM element: 

```clojure
(basic-rect app-state options)
```

where app-state is an application state and options are usually coordinates {:left ... :top ...}  relative to enclosing DOM element.

## Screenshots

## License

Copyright © 2018, **Wojciech Maka**

[1]: https://github.com/tailrecursion/boot
[2]: https://github.com/technomancy/leiningen
[3]: http://localhost:8000
