(ns lcmap.see.backend.mesos.models.sample-docker
  "This sample runner demonstrates kicking off a docker-based job.

  Note that the function defined in this namespace conforms to the standard
  defined for SEE model execution; it doesn't implement any spec, standard,
  or other best practice for Mesos itself. As such, this represents the
  boundary between SEE and Mesos. The `run` function that `run-model` in this
  namespace references, on the other hand, *does* represent one of the common
  ways in which Mesomatic Mesos frameworks are implemented, in particular as
  part of a namespace that defines a `-main` function for use as an executable
  .jar file. As such, that function (and namespace) represnts the other side
  of the SEE-Mesos boundary that is 100% Mesos, with no SEE influence
  whatsoever."
  (:require [clojure.tools.logging :as log]
            [lcmap.see.backend.mesos.models.sample-docker.framework :as framework]
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

(defn run-model [backend-impl model-name docker-tag]
  (let [tracker-impl (tracker/new model-name backend-impl)
        model-wrapper #'framework/run
        model-args [backend-impl tracker-impl model-name docker-tag]]
    (log/trace "Passing model args to tracker:" model-args)
    (tracker/track-job
      tracker-impl
      model-wrapper
      model-args)))
