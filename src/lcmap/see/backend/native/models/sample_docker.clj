(ns lcmap.see.backend.native.models.sample-docker
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]))

(defn exec-docker-run
  "This function is ultimately called by a Job Tracker implementation (usually
  `start-run-job`), which is what passes the `job-id` argument. The remaining
  args are what get set in the `run-model` function below."
  [job-id [model-name docker-tag year]]
  (log/debugf "\n\nRunning job (executing docker tag %s) ...\n"
              docker-tag)
  (let [cmd ["/usr/bin/sudo" "/usr/bin/docker"
             "run" "-t" docker-tag
             "--year" year]
        result @(exec/sh cmd)]
    (case (:exit result)
      0 (:out result)
      1 (:err result)
      [:error "unexpected output" result])))

(defn run-model [backend-impl model-name docker-tag year]
  (let [cfg (:cfg backend-impl)
        tracker-impl (tracker/new model-name backend-impl)
        model-wrapper #'exec-docker-run
        model-args [docker-tag year]]
    (log/trace "Args:" model-args)
    (tracker/track-job
      tracker-impl
      model-wrapper
      model-args)))

