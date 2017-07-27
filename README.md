# Cljs-Relational-Designer

This is going to be (I hope) a small cljs reagent relational designer tool.
A minimal plan is to have following functionalities:
- creating general purpose schema-less nodes. By schema-less I mean data which is going to be associated with nodes does not need to conform to any schema. The only schema of node entities are relations and attributes attributes (for attributes explanation refer below).   
- defining new attributes and attribute categories.
- aggregating attributes within entites. Possible entitities attributes categories are : name, description, priority, status.
- creating relations and making relationships between nodes.
- relations would be one-directional, bi-directional, and without direction.
- aggregating attributes within relations. (e.g. relation-type attribute category instance: relation-type : relies-on | depends-on | is-child-of | happens-after )
- in minimal plan app will not be complianat with UML specs
- Api for retrieval of data from nodes (retrieveing actual attribute values, narrowing relationships)



## Dependencies

- java 1.8+
- [boot][1]


## Usage

Need to reach minimal set of functionalities to be usable.

## License

Copyright © 2017, **Wojciech Maka**

[1]: https://github.com/tailrecursion/boot
[2]: https://github.com/technomancy/leiningen
[3]: http://localhost:8000
