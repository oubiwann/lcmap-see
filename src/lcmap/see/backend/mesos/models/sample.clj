(ns lcmap.see.backend.mesos.models.sample
  "This sample runner demonstrates kicking off a job that is executed on
  Mesos with a synthetic (and variable) delay introduced to show asynchronous
  results."
  (:require [clojure.tools.logging :as log]
            [lcmap.see.backend.mesos.models.sample.framework :as framework]
            [lcmap.see.job.tracker :as tracker]
            ;; The following line is CRUCIAL in order to resolve constructors
            [lcmap.see.job.tracker.mesos]))

;; Developer caution! -- It may be tenmpting to this "this could just be
;; a method of the backend, for the IModelable protocol ..."
;;
;; But! Remember: IModelable is not the same as an IModel ... backends are
;; used to *lookup* models (and call them), not *be* models. If you want
;; to use call the following function as a method, you will need to create
;; a new IModel protocol and associated implementations *for each model*.
;; It's probably more efficient just to use a function ...

(defn run-model [backend-impl model-name seconds year]
  (let [tracker-impl (tracker/new model-name backend-impl)
        model-wrapper #'framework/run
        model-args [backend-impl tracker-impl model-name seconds year]]
    (log/trace "Passing model args to tracker:" model-args)
    (tracker/track-job
      tracker-impl
      model-wrapper
      model-args)))
