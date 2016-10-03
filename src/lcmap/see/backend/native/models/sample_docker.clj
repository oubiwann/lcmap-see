(ns lcmap.see.backend.native.models.sample-docker
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]))

(defn exec-docker-run [[job-id docker-tag year]]
  (log/debugf "\n\nRunning job %s (executing docker tag %s) ...\n"
              job-id
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
  ;; Define some vars for pedagogical clarity
  (let [cfg (:cfg backend-impl)
        tracker-impl (tracker/new model-name backend-impl)
        model-func #'exec-docker-run
        model-args [job-id docker-tag year]]
    (log/trace "Args:" model-args)
    (tracker/track-job
      tracker-impl
      model-func
      model-args)))

