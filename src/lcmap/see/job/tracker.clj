(ns lcmap.see.job.tracker
  (:import [clojure.lang Keyword]))

(def tracker-ns "lcmap.see.job.tracker.")
(def dispatch-function "dispatch-handler")
(def tracker-function "track-job")

(defn get-tracker-ns
  "This utility function defines the standard namespace for SEE backend
  constructors. The namespace is assembled from constants and two passed
  arguments."
  [^Keyword backend]
  (str tracker-ns (name backend)))

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

(defprotocol ITrackable
  "An interface for job trackers which need to perform various duties such
  as running job functions, storing results, and returning results without
  running a job, if the job has already been run."
  (tbd [this]
    ""))

(def trackable-default-behaviour
  "Default implementations for ITrackable."
  {:tbd (fn [this] this)})
