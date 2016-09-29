(ns lcmap.see.backend
  (:require [clojure.tools.logging :as log]
            [lcmap.see.backend.base :as base])
  (:refer-clojure :exclude [new]))

(defn new
  ""
  [cfg db-conn event-thread]
  (let [constructor (base/get-constructor-fn (:backend cfg))]
    (log/debug "Got constructor:" constructor)
    (constructor cfg db-conn event-thread)))

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
    to the function that can be called to run a given model.")
  (run-model [this args]
    "Given the namespace and the function name (as strings), call the
    backend's `run-model` for this given model."))

(def componentable-default-behaviour
  "Default implementations for IComponentable."
  {:set-up #'base/set-up
   :tear-down #'base/tear-down})

(def modelable-default-behaviour
  "Default implementations for IModelable."
  {:get-model #'base/get-model
   :run-model #'base/run-model})

