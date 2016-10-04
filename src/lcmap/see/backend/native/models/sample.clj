(ns lcmap.see.backend.native.models.sample
  "This sample runner demonstrates kicking off a job that is executed on the
  system local to the LCMAP REST server, capturing standard out, with a
  synthetic (and variable) delay introduced to show asynchronous results."
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]
            ;; The following line is CRUCIAL in order to resolve constructors
            [lcmap.see.job.tracker.native]))

(defn long-running-func [job-id [model-name sleep-time year]]
  "This function is ultimately called by a Job Tracker implementation (usually
  `start-run-job`), which is what passes the `job-id` argument. The remaining
  args are what get set in the `run-model` function below."
  (log/debugf
    (str "\n\nRunning model '%s' with job-id '%s' (waiting for %s seconds)"
         " ...\n")
     model-name job-id sleep-time)
  ;; XXX can we just pass an int here? do we have to stringify?
  @(exec/sh ["sleep" (str sleep-time)])
  (:out @(exec/sh ["cal" year])))

;; Developer caution! -- It may be tenmpting to this "this could just be
;; a method of the backend, for the IModelable protocol ..."
;;
;; But! Remember: IModelable is not the same as an IModel ... backends are
;; used to *lookup* models (and call them), not *be* models. If you want
;; to use call the following function as a method, you will need to create
;; a new IModel protocol and associated implementations *for each model*.
;; It's probably more efficient just to use a function ...

(defn run-model [backend-impl model-name sleep-time year]
  (log/debug "Preparing to run sample model ...")
  (let [tracker-impl (tracker/new model-name backend-impl)
        model-wrapper #'long-running-func
        model-args [model-name sleep-time year]]
    (log/trace "Passing model args to tracker:" model-args)
    (tracker/track-job
      tracker-impl
      model-wrapper
      model-args)))
