(ns lcmap.see.components.job
  "LCMAP SEE job tracking component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [co.paralleluniverse.pulsar.actors :as actors]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.base :as base]
            [lcmap.see.job.tracker.native]))

(defrecord JobTracker []
  component/Lifecycle

  (start [component]
    (let [see-cfg (get-in component [:cfg :lcmap.see])
          db-conn (get-in component [:jobdb :conn])
          event-thread (actors/spawn (actors/gen-event))]
      (log/infof "Starting LCMAP SEE job tracker (%s) ..." (:backend see-cfg))
      (log/debug "Component keys:" (keys component))
      (log/debugf "db-conn: %s (%s)" db-conn (type db-conn))
      (let [tracker-impl (tracker/new see-cfg db-conn event-thread)]
        (tracker/connect-dispatch! tracker-impl)
        (log/debug "Tracker implementation:" tracker-impl)
        (assoc component :tracker tracker-impl))))

  (stop [component]
    (log/info "Stopping LCMAP SEE job tracker ...")
    (log/debug "Component keys" (keys component))
    (if-let [tracker-impl (:tracker component)]
      (tracker/stop tracker-impl))
    (assoc component :tracker nil)))

(defn new-job-tracker []
  (->JobTracker))
