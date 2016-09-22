(ns lcmap.see.backend.core
  (:import [clojure.lang Keyword]))

(def backend-ns "lcmap.see.backend.")
(def models-infix ".models.")
(def run-function "run-model")

(defn get-model-ns
  "This utility function defines the standard namespace for SEE science models.
  The namespace is assembled from constants and two passed arguments."
  [^Keyword backend ^String model-name]
  (str backend-ns (name backend) models-infix model-name))

(defn get-model-fn
  "This utility function uses the get-model-ns function to define the standard
  for full namespace + function name for SEE science models."
  [^Keyword backend ^String model-name]
  (->> run-function
       (str "/")
       (str (get-model-ns backend model-name))
       (symbol)
       (resolve)))

(defprotocol IComponentable
  "An interface for backends which need to be stopped and started as part of a
  system component."
  (set-up [this]
    "Connecting clients, instantiating drivers, or starting services.")
  (tear-down [this]
    "Reversing everything done in set-up."))

(defprotocol IModelable
  "An interface for backends which need to provide a means of getting model
  information."
  (get-model [this model-name]
    "Given the namespace and the function name (as strings), return a reference
    to the function that can be called to run a given model."))

(def componentable-default-behaviour
  "Default implementations for IComponentable."
  {:set-up (fn [this] this)
   :tear-down (fn [this] this)})

(def modelable-default-behaviour
  "Default implementations for IModelable."
  {:get-model (fn [this model-name] (get-model-fn (:name this) model-name))})
