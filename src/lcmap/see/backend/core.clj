(ns lcmap.see.backend.core
  (:import [clojure.lang Keyword]))

(defprotocol ExecutionBackend
  "An interface that all backends need to implement."
  (set-up [this]
    "Connecting clients, instantiating drivers, or starting services.")
  (tear-down [this]
    "Reversing everything done in set-up.")
  (get-model [this model-name]
    "Given the namespace and the function name (as strings), return a reference
    to the function that can be called."))

(defn get-models-ns
  ""
  [^Keyword backend ^String model-name]
  (str "lcmap.see.backend." (name backend) ".models." model-name))
