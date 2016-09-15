(ns lcmap.see.backend.native
  (:require [lcmap.see.backend.core :refer [ExecutionBackend]]))

(defrecord NativeBackend [name cfg]
  ExecutionBackend
  (set-up [this] this)
  (tear-down [this] this))

(defn new-backend
  ""
  [cfg]
  (->NativeBackend :native cfg))
