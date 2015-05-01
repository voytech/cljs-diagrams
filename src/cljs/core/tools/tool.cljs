(ns core.tools.tool)

(def tools (atom {}))

(defrecord Tool [name
                 desc
                 type
                 icon
                 func-ctor])

(defn create-tool
  ([name desc type icon func-ctor]
     (let [tool (Tool. name desc type icon func-ctor)]
       (swap! tools assoc name tool)
       tool))
  ([name type func-ctor]
     (create-tool name "?" type nil func-ctor))
  ([name desc type func-ctor]
     (create-tool name desc type nil func-ctor)))

(defn by-name [name]
  (get @tools name))
