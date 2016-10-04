(ns lcmap.see.job.tracker.mesos
  ""
  (:require [clojure.tools.logging :as log]
            [co.paralleluniverse.pulsar.core :refer [defsfn]]
            [co.paralleluniverse.pulsar.actors :as actors]
            [clojusc.twig :refer [pprint]]
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
  (log/debugf "Running the job '%s' with function %s and args %s ..."
              job-id
              job-func
              job-args)
  (db/update-status (:db-conn this) job-id status/pending-link)
  ;; XXX What's the best way to capture the results from a Mesos point of view?
  ;;     We'll probably need to send the result when the appropriate async
  ;;     handler fires ... so the handler might need a reference to the protocol
  ;;     implementation ... in which case *it* can notify the :event-thread
  ;;     of results available to save ...
  ;;
  ;; Maybe send send-msg func and {:type :job-finish-run}? then the backend can
  ;; call that when it's got a result?
  ;;
  ;; Better yet: define a callback partial that only needs to be passed the
  ;; final results (everything else is ready to go, including the messsage-
  ;; sending to the event thread).
  (job-func job-id job-args)
  (log/debugf "Kicked off Mesos framework with job-id:" job-id)
  ;; XXX send message!
  )

(defsfn finish-job-run
  [this {job-id :job-id job-result :result :as args}]
  (log/debugf "Got result of type %s with value %s" (type job-result) job-result)
  (log/debug "Finished job.")
  (base/send-msg this (into args {:type :job-save-data})))

(defsfn dispatch-handler
  [this {type :type :as args}]
  (log/debugf "Dispatching message of type '%s':" type)
  (log/trace "Message:" (pprint args))
  (case type
    :job-track-init (tracker/init-job-track this args)
    :job-result-exists (tracker/return-existing-result this args)
    :job-start-run (start-job-run this args)
    :job-finish-run (finish-job-run this args)
    :job-save-data (tracker/save-job-data this args)
    :job-track-finish (tracker/finish-job-track this args)
    :job-done (tracker/done this args)))

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
  [name cfg db-conn event-thread]
  (->MesosTracker name cfg db-conn event-thread))
