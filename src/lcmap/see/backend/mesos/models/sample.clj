(ns lcmap.see.backend.mesos.models.sample
  "This sample runner demonstrates kicking off a job that is executed on
  Mesos with a synthetic (and variable) delay introduced to show asynchronous
  results."
  (:require [clojure.tools.logging :as log]
            [lcmap.see.backend.mesos.models.sample.framework :as framework]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.mesos]))

(defn run-model [backend-impl model-name seconds year]
  (let [cfg (:cfg backend-impl)
        tracker-impl (tracker/new
                       model-name
                       (:cfg backend-impl)
                       (:db-conn backend-impl)
                       (:event-thread backend-impl))
        model-func #'framework/run
        model-args [backend-impl tracker-impl seconds year]]
    (log/debug "Preparing to run model ...")
    (tracker/track-job
      tracker-impl
      model-func
      model-args)))
