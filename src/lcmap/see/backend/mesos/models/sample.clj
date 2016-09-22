(ns lcmap.see.backend.mesos.models.sample
  "This sample runner demonstrates kicking off a job that is executed on the
  system local to the LCMAP REST server, capturing standard out, with a
  synthetic (and variable) delay introduced to show asynchronous results."
  (:require [clojure.tools.logging :as log]
            [lcmap.see.backend.mesos.models.sample.framework :as framework]
            [lcmap.see.job.tracker :as jt]))

(defn run-model [component job-id default-row result-table seconds year]
  ;; Define some vars for pedagogical clarity
  (let [args [component job-id seconds year]]
    ;; run mesos framework
    (apply framework/run args)
    ;; get results
    ;; save results to db
    (jt/track-job component
                  job-id
                  default-row
                  result-table
                  [func args])))
