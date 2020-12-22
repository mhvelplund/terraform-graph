(ns terraform-graph.xgml (:require [clojure.data.xml :as xml]))

(defn- attribute
  "Make a XGML attribute element"
  [key type value]
  (xml/element :attribute {:key key :type type} value))

(defn- section
  "Make a XGML attribute element"
  [name children]
  (xml/element :section {:name name} children))

(defn- resource-node
  "Convert a node descriptor to a XML node object"
  [name->module node]
  (let [module (:module node)
        parent (if (= module "") nil (:id (get name->module module)))
        base_content [(attribute "id" "int" (:id node))
                      (section "graphics"
                               [(attribute "type" "String" "rectangle")
                                (attribute "fill" "String" "#FFCC00")
                                (attribute "outline" "String" "#000000")])
                      (section "LabelGraphics"
                               [(attribute "text" "String" (:short_name node))
                                (attribute "fontSize" "int" 13)
                                (attribute "fontStyle" "String" "bold")
                                (attribute "name" "String" "Dialog")
                                (attribute "anchor" "String" "t")])
                      (section "LabelGraphics"
                               [(attribute "text" "String" (:type node))
                                (attribute "fontSize" "int" 12)
                                (attribute "name" "String" "Dialog")
                                (attribute "anchor" "String" "b")])]
        content (if (= parent nil)
                  base_content
                  (conj base_content (attribute "gid" "int" parent)))]
    (section "node" content)))

(defn- edge-node
  "Convert the `node` descriptor to a XML edge object and append it to the list of `edges`"
  [node_name->id edges node]
  (conj edges
        (map #(section "edge" [(attribute "source" "int" (:id node))
                               (attribute "target" "int" (node_name->id %))
                               (section "graphics" [(attribute "fill" "String" "#000000")
                                                    (attribute "targetArrow" "String" "standard")])
                               (section "edgeAnchor" [(attribute "xSource" "double" 1.0)
                                                      (attribute "xTarget" "double" -1.0)])])
             (:dependencies node))))

(defn- group-node
  "Convert a module descriptor to a XML node object"
  [node]
  (let [parent (:parent node)
        base_content [(attribute "id" "int" (:id node))
                      (section "graphics"
                               [(attribute "type" "String" "rectangle")
                                (attribute "hasFill" "boolean" false)
                                (attribute "outline" "String" "#000000")
                                (attribute "outlineWidth" "int" 2)
                                (attribute "outlineStyle" "String" "dashed")])
                      (section "LabelGraphics"
                               [(attribute "text" "String" (:short_name node))
                                (attribute "fontSize" "int" 15)
                                (attribute "fontName" "String" "Dialog")
                                (attribute "alignment" "String" "right")
                                (attribute "autoSizePolicy" "String" ">node_width</attribute>")
                                (attribute "anchor" "String" "t")
                                (attribute "borderDistance" "double" 0.0)])
                      (attribute "isGroup" "boolean" true)]
        content (if (= parent nil)
                  base_content
                  (conj base_content (attribute "gid" "int" parent)))]
    (section "node" content)))


;; 
(defn make-xgml
  "Transform a parsed datastructure to clojure.xml data"
  [parsed_state name->module]
  (let [nodes (vals (:nodes parsed_state))
        ;; modules_xgml (reduce module-node name->module)
        groups_xgml (map group-node (vals name->module))
        nodes_xgml (map (partial resource-node name->module) nodes)
        edges_xgml (reduce (partial edge-node (:ids parsed_state)) [] nodes)]
    (section "xgml" (section "graph" (concat groups_xgml nodes_xgml edges_xgml)))))