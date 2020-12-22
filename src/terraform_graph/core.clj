(ns terraform-graph.core
  (:gen-class)
  (:require [terraform-graph.xgml :as xgml]
            [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [clojure.string :as string]))

(defn read-state [file] (json/read-str (slurp file) :key-fn keyword))

(defn merge-dependencies
  "Takes a vector of resource instance-descriptors and returns a set of their combined dependencies"
  [instances]
  (reduce into #{} (filter some? (map :dependencies instances))))

(defn short-module-name
  "Strip the long form of module names down to just the names"
  [long_module_name]
  (loop [[module name & remaining] (string/split (or long_module_name "") #"\.")
         module_names []]
    (if (empty? module)
      (string/join "." module_names)
      (recur remaining (conj module_names (if (= module "module") name (str module "." name)))))))

(defn get-module-nesting
  "Turns \"lorem.ipsum.dolor\" into [\"lorem\" \"lorem.ipsum\" \"lorem.ipsum.dolor\"]"
  [module_name]
  (let [parts (clojure.string/split (or module_name "") #"\.")]
    (loop [[part & rest] parts
           last_key nil
           keys []]
      (if (empty? part)
        keys
        (let [key (if (empty? last_key) part (str last_key "." part))]
          (recur rest key (conj keys key)))))))

(defn map-modules
  "Create a map of module names with ids and the id of their parent"
  [module_names resources_count]
  (let [full_module_name_list (apply concat (map get-module-nesting module_names))]
    (loop [[module_name & rest] full_module_name_list
           name->module {}
           index resources_count]
      (if (empty? module_name)
        name->module
        (let [existing? (get name->module module_name)
              updated_name->module (if existing? name->module
                                       (let [name_parts (string/split module_name #"\.")
                                             delim (- (count name_parts) 1)
                                             parent_name (string/join "." (subvec name_parts 0 delim))
                                             short_name (get name_parts delim)]
                                         (assoc name->module
                                                module_name
                                                {:id index
                                                 :parent (:id (get name->module parent_name))
                                                 :short_name short_name})))
              updated_index (if existing? index (inc index))]
          (recur rest updated_name->module updated_index))))))

(defn parse
  "Read through the file and build a dependency graph data structure."
  [file]
  (let [resources (:resources (read-state file))]
    (loop [[resource & remaining_resources] resources ; the list being parsed
           id->node {} ; a map of nodes
           node_name->id {} ; the list of nodes being built
           index 0 ;; next id
           ]
      (if (empty? resource)
        ;; Return value
        {:nodes id->node
         :ids node_name->id}
        ;; Add a node to the result
        (let [module (short-module-name (:module resource))
              type (:type resource)
              short_name (:name resource)
              name (str (if (empty? module) "" (str module ".")) type "." short_name)
              node {:id index
                    :name name
                    :short_name short_name
                    :module module
                    :type type
                    :dependencies (into #{} (map short-module-name (merge-dependencies (:instances resource))))}]
          (recur remaining_resources (assoc id->node index node) (assoc node_name->id name index) (inc index)))))))

(defn -main
  "Generate a GML file representing the dependency graph of a statefile"
  [& args]
  (if (< (count args) 1)
    ((println "Usage: terraform-graph [input.json] <output.xgml>")
     (println)
     (println "  If no output filename is provided, default is stdout.")
     (System/exit 1))
    (let [parsed_state (parse (first args))
          nodes (:nodes parsed_state)
          name->module (map-modules (map :module (vals nodes)) (count nodes))
          xgml (xgml/make-xgml parsed_state name->module)]
      (if (< (count args) 2)
        (print (xml/indent-str xgml))
        (spit (second args) (xml/indent-str xgml))))))

;; (use '[clojure.repl])
;; (doc reduce)
;; https://github.com/clojure-cookbook/clojure-cookbook/blob/master/04_local-io/4-22_read-write-xml.asciidoc