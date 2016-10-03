(ns lcmap.see.backend.native.models.sample
  "This sample runner demonstrates kicking off a job that is executed on the
  system local to the LCMAP REST server, capturing standard out, with a
  synthetic (and variable) delay introduced to show asynchronous results."
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]))

(defn long-running-func [[sleep-time year]]
  (log/debugf "\n\nRunning job (waiting for %s seconds) ...\n"
              sleep-time)
  @(exec/sh ["sleep" (str sleep-time)])
  (:out @(exec/sh ["cal" year])))

;; Developer caution! -- It may be tenmpting to this "this could just be
;; a method of the backend, for the IModelable protocol ..."
;;
;; But! Remember: IModelable is not the same as IModel ... backends are
;; used to *lookup* models (and call them), not *be* models. If you want
;; to use call the following function as a method, you will need to create
;; a new IModel protocol and associated implementations *for each model*.
;; It's probably more efficient just to use a function ...

(defn run-model [backend-impl [model-name sleep-time year]]
  (let [cfg (:cfg backend-impl)
        tracker-impl (tracker/new model-name backend-impl)
        model-func #'long-running-func
        model-args [sleep-time year]]
    (log/trace "Passing model args to tracker:" model-args)
    (tracker/track-job
      tracker-impl
      model-func
      model-args)))
