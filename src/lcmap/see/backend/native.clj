(ns lcmap.see.backend.native
  (:require [lcmap.see.backend.core :as see]))

(defrecord NativeBackend [name cfg]
  see/ExecutionBackend
  (set-up [this]
    this)
  (tear-down [this]
    this)
  (get-model [this model-name]
    (-> this
        :name
        (see/get-models-ns model-name)
        (symbol)
        (ns-resolve (symbol "run-model")))))

(defn new-backend
  ""
  [cfg]
  (->NativeBackend :native cfg))
