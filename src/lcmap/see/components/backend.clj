(ns lcmap.see.components.backend
  "The LCMAP SEE component for selecting execution environment backend."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [lcmap.see.backend.core :as backend]
            [lcmap.see.backend.ec2 :as ec2]
            [lcmap.see.backend.mesos :as mesos]
            [lcmap.see.backend.native :as native]
            [lcmap.see.backend.nexus :as nexus]))

(defn select-backend
  ""
  [cfg]
  (case (:backend cfg)
    "ec2" nil
    "mesos" nil
    "native" (native/new-backend cfg)
    "nexus" nil))

(defrecord LCMAPSEEBackend []
  component/Lifecycle

  (start [component]
    (log/info "Starting LCMAP SEE execution backend ...")
    (let [see-cfg (get-in component [:cfg :lcmap.see])]
      (log/debug "Using config:" see-cfg)
      (let [backend-impl (select-backend see-cfg)]
        (backend/set-up backend-impl)
        (log/debug "Component keys:" (keys component))
        (log/debug "Successfully started LCMAP SEE backend:" backend-impl)
        (assoc component :backend backend-impl))))

  (stop [component]
    (log/info "Shutting down LCMAP SEE execution backend ...")
    (log/debug "Component keys" (keys component))
    (if-let [backend-impl (:backend component)]
      (do (log/debug "Using connection object:" backend-impl)
          (backend/tear-down backend-impl)
          ))
    (assoc component :backend nil)))

(defn new-backend []
  (->LCMAPSEEBackend))
