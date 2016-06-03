(ns lcmap.see.components.db
  "Database LCMAP REST Service system component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.client :as cc]
            [lcmap.config.cassaforte :refer [connect-opts]]))

(defrecord JobTrackerDBClient [ ]
  component/Lifecycle

  (start [component]
    (log/info "Starting LCMAP SEE DB client ...")
    (let [db-cfg (get-in component [:cfg :lcmap.see])]
      (log/debug "Using config:" db-cfg)
      (let [conn (apply cc/connect (connect-opts db-cfg))]
        (log/debug "Component keys:" (keys component))
        (log/debug "Successfully created db connection:" conn)
        (assoc component :conn conn))))

  (stop [component]
    (log/info "Stopping LCMAP SEE DB client ...")
    (log/debug "Component keys" (keys component))
    (if-let [conn (:conn component)]
      (do (log/debug "Using connection object:" conn)
          (cc/disconnect conn)))
    (assoc component :conn nil)))

(defn new-job-client []
  (->JobTrackerDBClient))
