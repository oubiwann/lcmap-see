(ns lcmap.see.backend.native.models.sample-docker
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]))

(defn exec-docker-run
  "This function is ultimately called by a Job Tracker implementation (usually
  `start-run-job`), which is what passes the `job-id` argument. The remaining
  args are what get set in the `run-model` function below.

  Note that if you wish to run this sample docker model wihtout having to enter
  a `sudo` password, you will need to either:

  1. start the SEE and/or the REST service as root, or
  2. configure sudo to be passwordless for the user that is running SEE

  Neither of these is a good solution for production deployments, but it is not
  expected that the native backend will be used for production, rather for
  development and testing."
  [job-id [model-name docker-tag]]
  (log/debugf "\n\nRunning job (executing docker tag %s) ...\n"
              docker-tag)
  (let [cmd ["/usr/bin/sudo" "/usr/bin/docker"
             "run" "-t" docker-tag]
        result @(exec/sh cmd)]
    (case (:exit result)
      0 (:out result)
      1 (:err result)
      [:error "unexpected output" result])))

;; Developer caution! -- It may be tenmpting to this "this could just be
;; a method of the backend, for the IModelable protocol ..."
;;
;; But! Remember: IModelable is not the same as an IModel ... backends are
;; used to *lookup* models (and call them), not *be* models. If you want
;; to use call the following function as a method, you will need to create
;; a new IModel protocol and associated implementations *for each model*.
;; It's probably more efficient just to use a function ...

(defn run-model [backend-impl model-name docker-tag]
  (let [cfg (:cfg backend-impl)
        tracker-impl (tracker/new model-name backend-impl)
        model-wrapper #'exec-docker-run
        model-args [model-name docker-tag]]
    (log/trace "Args:" model-args)
    (tracker/track-job
      tracker-impl
      model-wrapper
      model-args)))

