(ns lcmap.see.backend.native
  (:require [lcmap.see.backend :as see]))

(defrecord NativeBackend [name cfg])

(extend NativeBackend see/IComponentable see/componentable-default-behaviour)
(extend NativeBackend see/IModelable see/modelable-default-behaviour)

(defn new-backend
  ""
  [cfg]
  (->NativeBackend :native cfg))
