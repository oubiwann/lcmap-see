(ns lcmap.see.components.job
  "LCMAP SEE job tracking component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [co.paralleluniverse.pulsar.actors :as actors]
            [lcmap.see.job.tracker]))

(defrecord JobTracker []
  component/Lifecycle

  (start [component]
    (log/info "Starting LCMAP SEE job tracker ...")
    (let [job-tracker (actors/spawn (actors/gen-event))]
      (actors/add-handler!
        job-tracker
        #'lcmap.see.job.tracker/dispatch-handler)
      (log/debug "Component keys:" (keys component))
      (log/debug "Successfully created job tracker" job-tracker)
      (assoc component :tracker job-tracker)))

  (stop [component]
    (log/info "Stopping LCMAP SEE job tracker ...")
    (log/debug "Component keys" (keys component))
    (if-let [job-tracker (:tracker component)]
      (actors/shutdown! (:tracker component)))
    (assoc component :tracker nil)))

(defn new-job-tracker []
  (->JobTracker))
