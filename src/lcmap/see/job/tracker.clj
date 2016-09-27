(ns lcmap.see.job.tracker
  (:require [clojure.tools.logging :as log]
            [co.paralleluniverse.pulsar.core :refer [defsfn]]
            [co.paralleluniverse.pulsar.actors :as actors]
            [clojurewerkz.cassaforte.cql :as cql]
            [lcmap.client.status-codes :as status]
            [lcmap.see.job.db :as db]
            [lcmap.see.job.tracker.base :as base]))

;;; Utility functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-event-mgr [component]
  (get-in component [:job :tracker]))

;;; Protocols and behaviours ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ITrackable
  "An interface for job trackers which need to perform lookups of metadata
  function names."
  (stop [this]
    "")
  (get-tracker [this]
    "")
  (track-job [this job-id default-row result-table func-args]
    "")
  (get-event-thread [this]
    "")
  (get-dispatch [this args]
    "")
  (connect-dispatch! [this]
    "")
  (get-conn [this]
    ""))

(defprotocol IJobable
  "An interface for job trackers which need to perform various duties such
  as running job functions, storing results, and returning results without
  running a job, if the job has already been run."
  (result-exists? [this result-table job-id]
    "")
  (init-job-track [this args]
    "")
  (return-existing-result [this args]
    "")
  (run-job [this args]
    "")
  (save-job-data [this args]
    "")
  (finish-job-track [this args]
    "")
  (done [this args]
    ""))

;; XXX Don't use the protocol functions to get a function and then call it, just call it

(def trackable-default-behaviour
  "Default implementations for ITrackable."
  ;; XXX maybe in next line use #(-> % ...) instead of (fn ...)?
  {:stop #'base/stop-event-thread
   :get-tracker (fn [this] (base/get-tracker-fn (:name this)))
   :track-job #'base/track-job
   :get-dispatch (fn [this] (base/get-dispatch-fn (:name this)))
   :connect-dispatch! (fn [this] (base/connect-dispatch! this))
   :get-event-thread (fn [this] (:event-thread this))
   :get-conn (fn [this] (:db-conn this))})

(def jobable-default-behaviour
  "Default implementations for IJobable."
  {:result-exists? #'base/result-exists?
   :init-job-track #'base/init-job-track
   :return-existing-result #'base/return-existing-result
   :run-job #'base/run-job
   :save-job-data #'base/save-job-data
   :finish-job-track #'base/finish-job-track
   :done #'base/done})
