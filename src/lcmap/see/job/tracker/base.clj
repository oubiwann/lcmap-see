(ns lcmap.see.job.tracker.base
  (:require [clojure.tools.logging :as log]
            [co.paralleluniverse.pulsar.core :refer [defsfn]]
            [co.paralleluniverse.pulsar.actors :as actors]
            [clojusc.twig :refer [pprint]]
            [digest]
            [lcmap.client.status-codes :as status]
            [lcmap.see.job.db :as db])
  (:import [clojure.lang Keyword]))

(declare send-msg gen-hash)

(def tracker-ns "lcmap.see.job.tracker.")
(def init-function "new-tracker")
(def dispatch-function "dispatch-handler")
(def tracker-function "track-job")

;;; Tracker behaviour function implementations ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defsfn connect-dispatch!
  ""
  [this]
  (let [event-thread (:event-thread this)
        backend (get-in this [:cfg :backend])
        dispatch-fn (get-dispatch-fn backend)
        dispatcher (partial dispatch-fn this)]
    (log/debugf "Adding handler '%s' to event-thread (%s) ..."
                dispatch-fn event-thread)
    (actors/add-handler! event-thread dispatcher)
    this))

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
    (send-msg this {:type :job-track-init
                    :job-id job-id
                    :default-row default-row
                    :result [model-func model-args]})
    job-id))

;;; Job behaviour function implementations ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsfn gen-hash
  ""
  [this func args]
  (log/tracef "Preparing to hash [func args]: [%s %s]" func args)
  (-> func
      (str)
      (vector)
      (into args)
      (str)
      (digest/md5)))

(defsfn result-exists?
  [this job-id]
  (log/debug "Preparing to check for presence of results ...")
  (let [db-conn (:db-conn this)
        results-keyspace (get-in this [:cfg :results-keyspace])
        results-table (get-in this [:cfg :results-table])]
    (log/trace "Got args:" db-conn results-table job-id)
    (case (first @(db/result? db-conn results-keyspace results-table job-id))
      [] false
      nil false
      true)))

(defsfn send-msg
  ""
  [this args]
  (let [event-thread (:event-thread this)]
    (log/debug "Sending message to event-thread:" event-thread)
    (log/trace "Message:" (pprint args))
    (actors/notify! event-thread args)))

(defsfn init-job-track
  [this {job-id :job-id default-row :default-row func-args :result :as args}]
  (log/debug "Starting job tracking ...")
  (let [db-conn (:db-conn this)]
    (if (result-exists? this job-id)
      (send-msg this (into args {:type :job-result-exists}))
      (do
        @(db/insert-default db-conn job-id default-row)
        (send-msg this (into args {:type :job-start-run}))))))

(defsfn return-existing-result
  [this args]
  (log/debug "Returning ID for existing job results ...")
  (send-msg this (into args {:type :job-done})))

(defsfn start-job-run
  [this args]
  {:error "You need to override this function."})

(defsfn finish-job-run
  [this args]
  {:error "You need to override this function."})

(defsfn save-job-data
  [this {job-id :job-id job-output :result :as args}]
  (let [db-conn (:db-conn this)
        results-keyspace (get-in this [:cfg :results-keyspace])
        results-table (get-in this [:cfg :results-table])]
    (log/tracef "Saving job data \n%s with id %s ..."
                       job-output
                       job-id
                       results-table)
    @(db/save-job-result
      db-conn results-keyspace results-table job-id job-output)
    (log/debug "Saved.")
    (send-msg this (into args {:type :job-track-finish}))))

(defsfn finish-job-track
  [this {job-id :job-id result :result :as args}]
  @(db/update-status (:db-conn this) job-id status/permanant-link)
  (log/trace "Updated job traking data with" result)
  (send-msg this (into args {:type :job-done})))

(defsfn done
  [this {job-id :job-id :as args}]
  (log/debugf "Finished tracking for job %s." job-id))
