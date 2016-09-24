(ns lcmap.see.backend.native.models.sample-docker
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.native]))

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

(defn run-model [component job-id default-row result-table docker-tag year]
  ;; Define some vars for pedagogical clarity
  (let [backend (get-in component [:see :backend :name])
        track-job (tracker/get-tracker-fn backend)
        func #'exec-docker-run
        args [job-id docker-tag year]]
    (log/trace "Backend: " backend)
    (log/trace "Tracker function:" track-job)
    (log/trace "Args:" args)
    (track-job component
               job-id
               default-row
               result-table
               [func args])))

