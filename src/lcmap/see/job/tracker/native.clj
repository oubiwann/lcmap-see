(ns lcmap.see.job.tracker.native
  ""
  (:require [clojure.tools.logging :as log]
            [co.paralleluniverse.pulsar.core :refer [defsfn]]
            [co.paralleluniverse.pulsar.actors :as actors]
            [clojurewerkz.cassaforte.query :as query]
            [lcmap.client.status-codes :as status]
            [lcmap.see.job.db :as db]
            [lcmap.see.job.tracker :as tracker])
  (:refer-clojure :exclude [promise await bean])
  (:import [co.paralleluniverse.common.util Debug]
           [co.paralleluniverse.actors LocalActor]
           [co.paralleluniverse.strands Strand]))

;;; Implementation overrides for native tracker ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsfn run-job
  [this {job-id :job-id [job-func job-args] :result :as args}]
  (log/debugf "Running the job with function %s and args %s ..."
              job-func
              job-args)
  (let [job-data (job-func job-args)]
    (log/debugf "Got result of type %s with value %s" (type job-data) job-data)
    @(db/update-status (:db-conn this) job-id status/pending-link)
    (log/debug "Finished job.")
    (actors/notify! (:event-thread this)
                    (into args {:type :job-save-data
                                :result job-data}))))

;; XXX Merge the following as a behaviour override

(defsfn dispatch-handler
  [this {type :type :as args}]
  (case type
    :job-track-init (tracker/init-job-track this args)
    :job-result-exists (tracker/return-existing-result this args)
    :job-run (run-job this args)
    :job-save-data (tracker/save-job-data this args)
    :job-track-finish (tracker/finish-job-track this args)
    :done (tracker/done this args)))

;; XXX Move the following into the parent namespace

(defn track-job
  [this job-id default-row result-table func-args]
  (let [db-conn (:db-conn this)
        event-server (:event-thread this)]
    (log/debug "Using event server" event-server "with db connection" db-conn)
    (actors/notify! event-server
                    {:type :job-track-init
                     :job-id job-id
                     :default-row default-row
                     :result-table result-table
                     :result func-args})))

;;; Native protocol setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def jobable-behaviour
  (merge
    tracker/jobable-default-behaviour
    {:run-job #'run-job}))

(defrecord NativeTracker [name cfg db-conn event-thread])

(extend NativeTracker tracker/ITrackable tracker/trackable-default-behaviour)
(extend NativeTracker tracker/IJobable jobable-behaviour)

(defn new-tracker
  ""
  [cfg db-conn event-thread]
  (->NativeTracker :native cfg db-conn event-thread))
