(ns lcmap.see.backend.native
  (:require [lcmap.see.backend :as see]))

(defrecord NativeBackend [name cfg db-conn event-thread])

(extend NativeBackend see/IComponentable see/componentable-default-behaviour)
(extend NativeBackend see/IModelable see/modelable-default-behaviour)

(defn new-backend
  ""
  [cfg db-conn event-thread]
  (->NativeBackend :native cfg db-conn event-thread))
