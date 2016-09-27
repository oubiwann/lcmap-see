(ns lcmap.see.job.tracker.native
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
  (let [job-data (job-func job-args)]
    (log/debugf "Kicked off native job.")
    (base/send-msg this (into args {:type :job-finish-run
                                    :result job-data}))))

(defsfn finish-job-run
  [this {job-id :job-id job-result :result :as args}]
    (log/debugf "Got result of type %s with value %s" (type job-result) job-result)
    @(db/update-status (:db-conn this) job-id status/pending-link)
    (log/debug "Finished job.")
    (base/send-msg this (into args {:type :job-save-data})))

(defsfn dispatch-handler
  [this {type :type :as args}]
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

(defrecord NativeTracker [name cfg db-conn event-thread])

(extend NativeTracker tracker/ITrackable tracker/trackable-default-behaviour)
(extend NativeTracker tracker/IJobable jobable-behaviour)

(defn new-tracker
  ""
  [cfg db-conn event-thread]
  (->NativeTracker :native cfg db-conn event-thread))
