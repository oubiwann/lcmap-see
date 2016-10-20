(ns lcmap.see.job.tracker.native
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

;;; Implementation overrides for native job ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsfn gen-hash
  "Generate a unique hash for science model function and specific parameters
  provided for the science model. This allows the SEE to provide results that
  have already been generated once without having to recalculate them."
  [this func args]
  (log/debugf "Preparing to hash [func args]: [%s %s]" func args)
  (-> func
      (str)
      (vector)
      (into args)
      (str)
      (digest/md5)))

(defsfn start-job-run
  [this {job-id :job-id [job-func job-args] :result :as args}]
  (log/debugf "Running the job with function %s and args %s ..."
              job-func
              job-args)
  @(db/update-status (:db-conn this) job-id status/pending-link)
  (let [job-data (job-func job-id job-args)]
    (log/debugf "Kicked off native job with job-id:" job-id)
    (base/send-msg this (into args {:type :job-finish-run
                                    :result job-data}))))

(defsfn finish-job-run
  [this {job-id :job-id job-result :result :as args}]
    (log/debugf "Got result of type %s with value %s" (type job-result) job-result)
    ;; Perform any post-run job clean up that is needed
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

;;; Implementation overrides for native tracker ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsfn track-job
  [this model-func model-args]
  (log/debug "Preparing to track job ...")
  (let [db-conn (:db-conn this)
        job-id (gen-hash this model-func model-args)
        default-row (db/make-default-row (:cfg this) job-id (:name this))]
    (log/debug "Generated model run hash (job-id):" job-id)
    (log/trace "Generated default-row:" (pprint default-row))
    (log/trace "Using event server" (:event-thread this) "with db connection"
               db-conn)
    (tracker/send-msg
      this {
        :type :job-track-init
        :job-id job-id
        :default-row default-row
        :result [model-func model-args]})
    job-id))

;;; Native protocol setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def trackable-behaviour
  (merge
    tracker/trackable-default-behaviour
    {:track-job #'track-job}))

(def jobable-behaviour
  (merge
    tracker/jobable-default-behaviour
    {:gen-hash #'gen-hash
     :start-job-run #'start-job-run
     :finish-job-run #'finish-job-run}))

(defrecord NativeTracker [name cfg db-conn event-thread])

(extend NativeTracker tracker/ITrackable trackable-behaviour)
(extend NativeTracker tracker/IJobable jobable-behaviour)

(defn new-tracker
  ""
  [name cfg db-conn event-thread]
  (->NativeTracker name cfg db-conn event-thread))
