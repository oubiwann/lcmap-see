(ns lcmap.see.job.tracker.base
  (:require [clojure.tools.logging :as log]
            [co.paralleluniverse.pulsar.core :refer [defsfn]]
            [co.paralleluniverse.pulsar.actors :as actors]
            [clojurewerkz.cassaforte.cql :as cql]
            [digest]
            [lcmap.client.status-codes :as status]
            [lcmap.see.job.db :as db]
            [lcmap.see.util :as util])
  (:import [clojure.lang Keyword]))

(declare send-msg)

(def tracker-ns "lcmap.see.job.tracker.")
(def init-function "new-tracker")
(def dispatch-function "dispatch-handler")
(def tracker-function "track-job")

;;; Tracker behaviour function implementations ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn stop-event-thread
  ""
  [this]
  (actors/shutdown! (:event-thread this)))

(defn get-tracker-ns
  "This utility function defines the standard namespace for SEE backend
  constructors. The namespace is assembled from constants and two passed
  arguments."
  [^Keyword backend]
  (str tracker-ns (name backend)))

(defn get-constructor-fn
  "This utility function uses the get-constructor-ns function to define
  the standard for full namespace + function name for SEE constructor
  functions."
  [^Keyword backend]
  (->> init-function
       (str "/")
       (str (get-tracker-ns backend))
       (symbol)
       (resolve)))

(defn get-dispatch-fn
  "This utility function uses the get-tracker-ns function to define
  the standard for full namespace + function name for SEE job tracker
  dispatch function."
  [^Keyword backend]
  (->> dispatch-function
       (str "/")
       (str (get-tracker-ns backend))
       (symbol)
       (resolve)))

(defn get-tracker-fn
  "This utility function uses the get-tracker-ns function to define
  the standard for full namespace + function name for the SEE job
  tracker function."
  [^Keyword backend]
  (->> tracker-function
       (str "/")
       (str (get-tracker-ns backend))
       (symbol)
       (resolve)))

(defn connect-dispatch!
  ""
  [this]
  (actors/add-handler!
    (:event-thread this)
    (partial (get-dispatch-fn (:name this)) this))
  this)

(defn track-job
  [this job-id default-row result-table func-args]
  (let [db-conn (:db-conn this)]
    (log/debug "Using event server" (:event-thread this) "with db connection"
               db-conn)
    (send-msg this {:type :job-track-init
                    :job-id job-id
                    :default-row default-row
                    :result-table result-table
                    :result func-args})))

;;; Job behaviour function implementations ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn gen-hash
  ""
  [this func args]
  (->> args
       (util/serialize)
       (str func)
       (symbol)
       (resolve)
       (str)
       (digest/md5)))

(defsfn result-exists?
  [this result-table job-id]
  (let [db-conn (:db-conn this)]
    (log/debug "Got args:" db-conn result-table job-id)
    (case (first @(db/result? db-conn result-table job-id))
      [] false
      nil false
      true)))

(defn send-msg
  ""
  [this args]
  (actors/notify! (:event-thread this) args))

(defsfn init-job-track
  [this {job-id :job-id default-row :default-row result-table :result-table
   func-args :result :as args}]
  (log/debug "Starting job tracking ...")
  (let [db-conn (:db-conn this)]
    (if (result-exists? this result-table job-id)
      (send-msg this (into args {:type :job-result-exists}))
      (do
        @(db/insert-default db-conn job-id default-row)
        (send-msg this (into args {:type :job-start-run}))))))

(defsfn return-existing-result
  [this args]
  (log/debug "Returning ID for existing job results ...")
  (send-msg this (into args {:type :job-done})))

(defn start-job-run
  [this args]
  {:error "You need to override this function."})

(defn finish-job-run
  [this args]
  {:error "You need to override this function."})

(defsfn save-job-data
  [this {job-id :job-id result-table :result-table job-output :result
   :as args}]
  (log/debugf "Saving job data \n%s with id %s to %s ..."
                     job-output
                     job-id
                     result-table)
  @(cql/insert-async
     (:db-conn this) result-table {:result_id job-id :result job-output})
  (log/debug "Saved.")
  (send-msg this (into args {:type :job-track-finish})))

(defsfn finish-job-track
  [this {job-id :job-id result :result :as args}]
  @(db/update-status (:db-conn this) job-id status/permanant-link)
  (log/debug "Updated job traking data with" result)
  (send-msg this (into args {:type :job-done})))

(defsfn done
  [this {job-id :job-id :as args}]
  (log/debugf "Finished tracking for job %s." job-id))
