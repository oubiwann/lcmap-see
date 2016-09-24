(ns lcmap.see.components.job
  "LCMAP SEE job tracking component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [co.paralleluniverse.pulsar.actors :as actors]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.native]))

(defrecord JobTracker []
  component/Lifecycle

  (start [component]
    (let [see-cfg (get-in component [:cfg :lcmap.see])
          backend (:backend see-cfg)
          event-thread (actors/spawn (actors/gen-event))
          dispatch-fn (tracker/get-dispatch-fn backend)]
      (log/infof "Starting LCMAP SEE job tracker (%s) ..." backend)
      (actors/add-handler! event-thread dispatch-fn)
      (log/debug "Component keys:" (keys component))
      (log/debug "Successfully created job tracker thread" event-thread)
      (assoc component :tracker event-thread)))

  (stop [component]
    (log/info "Stopping LCMAP SEE job tracker ...")
    (log/debug "Component keys" (keys component))
    (if-let [job-tracker (:tracker component)]
      (actors/shutdown! (:tracker component)))
    (assoc component :tracker nil)))

(defn new-job-tracker []
  (->JobTracker))
