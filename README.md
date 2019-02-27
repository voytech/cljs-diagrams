# Cljs-Relational-Designer
## Rationale
Clojurescript library/framework for creating different kinds of diagrams. Primarily for use as dependency for cljs apps. 

Why cljs library ? 
I cannot find answer for that question. This decision most likely tightens much potential usage, because when I would have to do some diagraming e.g. in js react or angular or vue based app, I would not consider this library as a dependency, just because it feels not natural to nest cljs in js application context. By design I would say that I support only cljs applications. 

So once again, Why cljs ? 
I think just because I want to keep learning this language, just because I like it, just because it gives me fun to play with it. Still I do not know why I am struggling so hard to make it working. I do not have any hope it will be used by someone. 

There is no rationale.    

## Features
Key functionalities:
- Library with wide range of reusable shapes (this is a joke of course!)
- Support for custom diagram shapes (One can create own shape and entities using macro based DSL)
- Creating various relationships between entities 
- Behaviours abstraction - event handlers (on steroids) with automatic source shape recognition via shape features preconditions, and auto-wiring of behaviours - inferring (and bus registration) of behaviours from entity specification (e.g. types of managed components) (All subject to change)
- Support for custom rendering engines 
- SVG renderer engine - for vanilla js
- Canvas renderer engine (using fabric.js) (to refactor)
- Pluggable architecture via event bus
- Simple, extensible shape layouting abstraction for constraining entity look and feel.
- Higher level, application data resolvers.  

## Dependencies

- java 1.8+
- [boot][1]

## Usage
Cljs-Relation-Designer is shipped with number of display components acting as building blocks for new shapes.
Types of components belonging to particular shape are determining shape features and behaviours. Including new components into primary entities (main shapes) gives more feature, more meaning and power, enabling new behaviours of the entity itself.

E.g.: 
- when one adds 'control' components to shape, shape will automatically become resizable and connectible with other shapes.
- when one adds 'text' component, text will automatically change its border colour on hover.
- when one adds 'startpoint' and 'endpoint' components into the shape - shape is becomming relation entity which can be bound to other rigid entities.
- when one add a tag named 'container' to the shape - it becomes a shape that can group other shapes.

You are not restricted to predefined set of features, components, tags, behaviours or data resolvers. All above abstractions can be turned of, or new implementations can be added. 

Below there is very basic usage scenario with minimal customisation. 
Supose You just want to add simple rectangular shape with just a title in the middle of its bounding box. 

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
c/entity-shape - is a basic component representing leading appearance for rigid shapes (e.g. for presenting diagram node like uml class, object, bpmn activity or event nodes.)

c/title - is also predefined component for presenting text. This is an alias for c/text component.

c/entity-controls is predefined component-group which enables some behaviours like resizing, making relations. 

And this is how to render this entity on DOM element: 

```clojure
(basic-rect app-state {:left 100 :top 100})
```

where app-state is an application state and options are usually coordinates {:left ... :top ...}  relative to enclosing DOM element. app-state must be firstly initialized by project/initialize method. 

## Screenshots

## License

Copyright © 2018, **Wojciech Maka**

[1]: https://github.com/tailrecursion/boot
[2]: https://github.com/technomancy/leiningen
[3]: http://localhost:8000
