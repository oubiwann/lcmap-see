(ns lcmap.see.components.backend
  "The LCMAP SEE component for selecting execution environment backend."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [lcmap.see.backend.ec2 :as ec2-backend]
            [lcmap.see.backend.mesos :as mesos-backend]
            [lcmap.see.backend.native :as native-backend]
            [lcmap.see.backend.nexus :as nexus-backend]))

(defn select-backend
  ""
  [cfg]
  :backend)

(defrecord LCMAPSEEBackend []
  component/Lifecycle

  (start [component]
    (log/info "Starting LCMAP SEE execution backend ...")
    (let [see-cfg (get-in component [:cfg :lcmap.see])]
      (log/debug "Using config:" see-cfg)
      (let [backend (select-backend see-cfg)]
        ;(backend/setup)
        (log/debug "Component keys:" (keys component))
        (log/debug "Successfully started LCMAP SEE backend:" backend)
        (assoc component :backend backend))))

  (stop [component]
    (log/info "Shutting down LCMAP SEE execution backend ...")
    (log/debug "Component keys" (keys component))
    (if-let [backend (:backend component)]
      (do (log/debug "Using connection object:" backend)
          ;(backend/teardown)
          ))
    (assoc component :backend nil)))

(defn new-backend []
  (->LCMAPSEEBackend))
