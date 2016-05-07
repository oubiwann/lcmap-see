(ns ^{:doc
  "Database LCMAP REST Service system component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."}
  lcmap.see.components.db
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.policies :as cp]))

(defrecord JobTrackerDBClient [ ]
  component/Lifecycle

  (start [component]
    (log/info "Starting LCMAP SEE DB client ...")
    (let [db-cfg (get-in component [:cfg :lcmap.see.components.db])]
      (log/debug "Using config:" db-cfg)
      (let [conn (cc/connect (:hosts db-cfg) (dissoc db-cfg :hosts))]
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
