(ns lcmap.see.backend.mesos.models.sample
  "This sample runner demonstrates kicking off a job that is executed on
  Mesos with a synthetic (and variable) delay introduced to show asynchronous
  results."
  (:require [clojure.tools.logging :as log]
            [lcmap.see.backend.mesos.models.sample.framework :as framework]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.mesos]))

(defn run-model [component job-id default-row result-table seconds year]
  ;; Define some vars for clarity
  (let [see-backend (get-in component [:see :backend])
        tracker-impl (get-in component [:see :job :tracker])
        func #'framework/run
        args [see-backend tracker-impl [job-id seconds year]]]
    (log/debug "Preparing to run model ...")
    (tracker/track-job
      tracker-impl
      job-id
      default-row
      result-table
      [func args])))
