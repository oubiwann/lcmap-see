(ns lcmap.see.job.db
  (:require [clojure.tools.logging :as log]
            [clojure.core.match :refer [match]]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as query]
            [lcmap.config.helpers :refer [init-cfg]]
            [lcmap.see.config :as see-cfg]
            [lcmap.see.util :as util]))

;; XXX Use components instead? This is makes using a test configuration
;;     somewhat difficult ...  in order for this to work, we need to pass
;;     the component instead of the connection -- sadly, the connection is
;;     being used *everywhere*, so this will touch a lot of code. Created
;;     LCMAP-549 to handle this.
(def cfg ((init-cfg see-cfg/defaults) :lcmap.see))
(def job-keyspace (:job-keyspace cfg))
(def job-table (:job-table cfg))

; (defn get-keyspace [conn]
;   )

; (defn get-table [conn]
;   )

(defn job? [conn job-id]
  ;(log/debug "Connection keys:" (keys conn))
  ;(log/debug "Connection cfg keys:" (keys (:cfg conn)))
  (cql/use-keyspace conn job-keyspace)
  (cql/select-async
    conn
    job-table
    (query/where [[= :job_id job-id]])
    (query/limit 1)))

(defn result? [conn result-table result-id]
  (log/debugf "Checking for result of id %s in table '%s' ..."
              result-id result-table)
  (cql/use-keyspace conn job-keyspace)
  (cql/select-async
    conn
    result-table
    (query/where [[= :result_id result-id]])
    (query/limit 1)))

(defn insert-default [conn job-id default-row]
  (log/debugf "Saving %s to '%s.%s' .." default-row job-keyspace job-table)
  (cql/use-keyspace conn job-keyspace)
  (cql/insert-async
    conn
    job-table
    (into default-row {:job_id job-id})))

(defn update-status [conn job-id new-status]
  (cql/use-keyspace conn job-keyspace)
  (cql/update-async conn
                    job-table
                    {:status new-status}
                    (query/where [[= :job_id job-id]])))


(defn get-job-result [db job-id result-table status-func]
  (match [(first @(result? (:conn db) result-table job-id))]
    [[]]
      (status-func db job-id)
    [nil]
      (status-func db job-id)
    [result]
      (status-func result)))
