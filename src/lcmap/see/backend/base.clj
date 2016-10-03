(ns lcmap.see.backend.base
  (:require [clojure.tools.logging :as log])
  (:import [clojure.lang Keyword]))

(def backend-ns "lcmap.see.backend.")
(def models-infix ".models.")
(def init-function "new-backend")
(def run-function "run-model")

;;; Component behaviour function implementations ;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-up
  ""
  [this]
  this)

(defn tear-down
  ""
  [this]
  this)

;;; Model behaviour function implementations ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-constructor-ns
  "This utility function defines the standard namespace for SEE backend
  constructors. The namespace is assembled from constants and two passed
  arguments."
  [^Keyword backend]
  (str backend-ns (name backend)))

(defn get-model-ns
  "This utility function defines the standard namespace for SEE science models.
  The namespace is assembled from constants and two passed arguments."
  [^Keyword backend ^String model-name]
  (str backend-ns (name backend) models-infix model-name))

(defn get-constructor-fn
  "This utility function uses the get-constructor-ns function to define
  the standard for full namespace + function name for SEE constructor
  functions."
  [^Keyword backend]
  (->> init-function
       (str "/")
       (str (get-constructor-ns backend))
       (symbol)
       (resolve)))

(defn get-model-fn
  "This utility function uses the get-model-ns function to define the standard
  for full namespace + function name for SEE science models."
  [^Keyword backend ^String model-name]
  (->> run-function
       (str "/")
       (str (get-model-ns backend model-name))
       (symbol)
       (resolve)))

(defn get-model
  "Obtain a function model via the lookup mechanism defined for all backends."
  [this model-name]
  (get-model-fn (:name this) model-name))

(defn run-model
  "This function is called to run a dynamically obtained model.

  It does so by first looking up the model name, using the first element
  of the `args` vector (which should always be the model name). Once the
  model (a function) is obtained, it is called by applying all the
  arguments in the `args` variable IN ADDITION TO prepending the backend
  implementation into the first position. Some models require data that
  only the backend has access to, so it is provided to ALL models.

  The model function that is looked up is the one expected to be at the
  following location:

    `lcmap.see.backend.<impl>.models.<model name>/run-model`

  The function that asks for this dynamically obtained model function is
  usually a function in `lcmap.rest.api.models.*.`"
  [this args]
  (let [model-name (first args)
        model (get-model this model-name)]
    (log/trace "Got original args:" args)
    (log/trace "Prepending args:" this)
    (apply model (into [this] args))))
