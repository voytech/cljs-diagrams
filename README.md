# Cljs-Relational-Designer

Cljs library for creating different kinds of diagrams.
Key functionalities:
- defining different kind of entities (diagram entity classes)
- creating relationships between entities
- feeding entities with content (represented by attribute values)
- behaviours - event handling abstraction
- auto-wiring of behaviours onto entities (via behaviour validators)
- different rendering engines (currently only canvas (via fabric.js), but will support SVG rendering)
- extensibility via event driven model (event bus)
- DSL for defining new entity classes (via macros : defentity, defcomponent, defatrribute, defbehaviour) .

## Dependencies

- java 1.8+
- [boot][1]


## Usage

Need to reach minimal set of functionalities to be usable.

## Screenshots

## License

Copyright © 2018, **Wojciech Maka**

[1]: https://github.com/tailrecursion/boot
[2]: https://github.com/technomancy/leiningen
[3]: http://localhost:8000
