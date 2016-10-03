(ns lcmap.see.backend.mesos
  (:require [clojure.core.async :as async :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lcmap.see.backend :as see]
            [lcmap.see.util :as util]))

;; The following record is designed to be used with the interfaces (protocols)
;; defined for generilzed SEE backends. The record should support data needed
;; by the backend implementations in order to perform the duties of a
;; component, a science model, and any other defined protocols.
(defrecord MesosBackend [name cfg db-conn])

(extend MesosBackend see/IComponentable see/componentable-default-behaviour)
(extend MesosBackend see/IModelable see/modelable-default-behaviour)

(defn new-backend
  ""
  [cfg db-conn]
  (->MesosBackend :mesos cfg db-conn))
