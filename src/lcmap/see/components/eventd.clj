(ns ^{:doc
  "Event LCMAP SEE system component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."}
  lcmap.see.components.eventd
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [co.paralleluniverse.pulsar.actors :as actors]
            [lcmap.see.job.tracker]))

(defrecord EventServer []
  component/Lifecycle

  (start [component]
    (log/info "Starting LCMAP SEE event server ...")
    (let [event-server (actors/spawn (actors/gen-event))]
      (actors/add-handler!
        event-server
        #'lcmap.see.job.tracker/dispatch-handler)
      (log/debug "Component keys:" (keys component))
      (log/debug "Successfully created event server:" event-server)
      (assoc component :eventd event-server)))

  (stop [component]
    (log/info "Stopping LCMAP SEE event server ...")
    (log/debug "Component keys" (keys component))
    (if-let [event-server (:eventd component)]
      (actors/shutdown! (:eventd component)))
    (assoc component :eventd nil)))

(defn new-event-server []
  (->EventServer))
