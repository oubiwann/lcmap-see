(ns lcmap.see.backend.base
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
  ""
  [this model-name]
  (get-model-fn (:name this) model-name))

(defn run-model
  ""
  [this model-name args]
  (apply (get-model this model-name) args))
