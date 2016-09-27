(ns lcmap.see.backend.mesos.models.sample
  "This sample runner demonstrates kicking off a job that is executed on
  Mesos with a synthetic (and variable) delay introduced to show asynchronous
  results."
  (:require [clojure.tools.logging :as log]
            [lcmap.see.backend.mesos.models.sample.framework :as framework]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.mesos]))

(defn run-model [component job-id default-row result-table seconds year]
  ;; Define some vars for pedagogical clarity
  (let [tracker-impl (get-in component [:see :job :tracker])
        func #'framework/run
        args [job-id seconds year]]
    ;; run mesos framework
    (apply framework/run args)
    ;; get results
    ;; save results to db
    (tracker/track-job
      tracker-impl
      job-id
      default-row
      result-table
      [func args])))
