(ns lcmap.see.components.job
  "LCMAP SEE job tracking component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [co.paralleluniverse.pulsar.actors :as actors]
            [lcmap.see.job.tracker.base :as base]
            [lcmap.see.job.tracker.mesos]
            [lcmap.see.job.tracker.native]))

(defrecord JobTracker []
  component/Lifecycle

  (start [component]
    (let [see-cfg (get-in component [:cfg :lcmap.see])
          db-conn (get-in component [:jobdb :conn])
          event-thread (actors/spawn (actors/gen-event))]
      (log/infof "Starting LCMAP SEE job event-thread (%s) ..." (:backend see-cfg))
      (log/debug "Component keys:" (keys component))
      (log/debugf "db-conn: %s (%s)" db-conn (type db-conn))
      (log/debug "Tracker implementation:" event-thread)
      (assoc component :event-thread event-thread)))

  (stop [component]
    (log/info "Stopping LCMAP SEE job event-thread ...")
    (log/debug "Component keys" (keys component))
    (if-let [event-thread (:event-thread component)]
      (actors/shutdown! event-thread))
    (assoc component :event-thread nil)))

(defn new-job-tracker []
  (->JobTracker))
