(ns lcmap.see.job.tracker.mesos
  ""
  (:require [clojure.tools.logging :as log]
            [co.paralleluniverse.pulsar.core :refer [defsfn]]
            [co.paralleluniverse.pulsar.actors :as actors]
            [lcmap.client.status-codes :as status]
            [lcmap.see.job.db :as db]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.base :as base])
  (:refer-clojure :exclude [promise await bean])
  (:import [co.paralleluniverse.common.util Debug]
           [co.paralleluniverse.actors LocalActor]
           [co.paralleluniverse.strands Strand]))

;;; Implementation overrides for native tracker ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsfn start-job-run
  [this {job-id :job-id [job-func job-args] :result :as args}]
  (log/debugf "Running the job with function %s and args %s ..."
              job-func
              job-args)
  ;; XXX What's the best way to capture the results from a Mesos point of view?
  ;;     We'll probably need to send the result when the appropriate async
  ;;     handler fires ... so the handler might need a reference to the protocol
  ;;     implementation ... in which case *it* can notify the :event-thread
  ;;     of results available to save ...
  (job-func job-args)
  (log/debugf "Kicked off Mesos framework."))

(defsfn finish-job-run
  [this {job-id :job-id job-result :result :as args}]
  @(db/update-status (:db-conn this) job-id status/pending-link)

  ;; XXX Maybe split this into two functions?
  ;;     - #'start-run-job (and state transition :job-start-run)
  ;;     - #'finish-run-job (and state transition :job-finish-run)
  ;;     This would allow us to more easily handle process other async execution
  ;;     frameworks for results. It will mean that the transitions (and related
  ;;     functions) will need to be updated (replacing :run-job with the two new
  ;;     transitions ...)
  (log/debug "Finished job.")
  (base/send-msg this (into args {:type :job-save-data}))))

(defsfn dispatch-handler
  [this {type :type :as args}]
  (case type
    :job-track-init (tracker/init-job-track this args)
    :job-result-exists (tracker/return-existing-result this args)
    :job-start-run (start-job-run this args)
    :job-finish-run (finish-job-run this args)
    :job-save-data (tracker/save-job-data this args)
    :job-track-finish (tracker/finish-job-track this args)
    :done (tracker/done this args)))

;;; Native protocol setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def jobable-behaviour
  (merge
    tracker/jobable-default-behaviour
    {:start-job-run #'start-job-run
     :finish-job-run #'finish-job-run}))

(defrecord MesosTracker [name cfg db-conn event-thread])

(extend MesosTracker tracker/ITrackable tracker/trackable-default-behaviour)
(extend MesosTracker tracker/IJobable jobable-behaviour)

(defn new-tracker
  ""
  [cfg db-conn event-thread]
  (->MesosTracker :native cfg db-conn event-thread))
