(ns lcmap.see.backend.mesos
  (:require [clojure.core.async :as async :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lcmap.see.backend.core :as see]
            [lcmap.see.util :as util]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; LCMAP SEE backend implementation for Mesos
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; The following record is designed to be used with the interfaces (protocols)
;;; defined for generilzed SEE backends. The record should support data needed
;;; by the backend implementations in order to perform the duties of a
;;; component, a science model, and any other defined protocols.

(defrecord MesosBackend [name cfg])

(extend MesosBackend see/IComponentable see/componentable-default-behaviour)
(extend MesosBackend see/IModelable see/modelable-default-behaviour)

(defn new-backend
  ""
  [cfg]
  (->MesosBackend :mesos cfg))
