(ns lcmap.see.backend.mesos.models.sample.framework
  "A sample Mesos framework for the LCMAP SEE.

  From the perspective of LCMAP SEE, this namespace needs only define one
  function: that which was defined and passed to the `track-job` function
  in `lcmap.see.backend.mesos.models.sample`.

  From the perspective of the Mesomatic async framework, this
  namespace needs to define all the scheduler handlers."
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
            [lcmap.see.backend.mesos.models.common.framework :as comm-framework]
            [lcmap.see.backend.mesos.models.sample.scheduler :as sample-scheduler]
            [lcmap.see.backend.mesos.util :as util]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Constants and Data
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; In a real application, most of these would be defined in an appropriate
;;; context, using application confgiration values, values extracted from
;;; passed state, etc. This is done for pedagogical purposes only: in an
;;; attempt to keep things clear and clean for the learning experience. Do
;;; not emulate in production code!

(def framework-info-map {:name "LCMAP SEE Sample Model (Mesos Framework)"
                         :principal "sample-framework"
                         :checkpoint true})

(def limits
  "Note that :max-tasks gets set via an argument passed to the `run` function."
  {:cpus-per-task 1
   :mem-per-task 128
   :max-tasks nil})

(defrecord FrameworkState [
  ;; Mesos State
  driver channel exec-info master-info framework-id offers tasks
  ;; LCMAP SEE State
  launched-tasks limits backend tracker model-name model-args see-job-id])

(defn new-state
  ""
  [driver ch backend tracker model-name model-args see-job-id]
  ;(map->FrameworkState
      {;; Mesos State
       :driver driver
       :channel ch
       :exec-info nil
       :master-info nil
       :framework-id nil
       :offers nil
       :tasks nil
       ;; LCMAP SEE State
       :launched-tasks 0
       ;; XXX remove or update limits-processing code
       :limits (assoc limits :max-tasks 2)
       :backend backend
       :tracker tracker
       :model-name model-name
       :model-args model-args
       :see-job-id see-job-id}
       ;)
)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  "This is the function that actually runs the framework.

  This function is ultimately called by a Job Tracker implementation (see
  the tracker's method `start-run-job`), which is what passes the `job-id`
  argument. The remaining args are what got passed to the tracker by
  `lcmap.see.backend.mesos.models.sample/run-model`."
  [see-job-id [backend-impl tracker-impl model-name sleep-time year]]
  (log/info "Running LCMAP SEE example model Mesos framework ...")
  (log/trace "Got backend:" backend-impl)
  (log/trace "Got tracker:" tracker-impl)
  (let [ch (async/chan)
        sched (async-scheduler/scheduler ch)
        master (util/get-master backend-impl)
        host (util/get-host backend-impl)
        driver (scheduler-driver
                 sched (assoc framework-info-map :hostname host)
                 master nil false)
        model-args [sleep-time year]
        state (new-state
                driver ch backend-impl tracker-impl model-name
                model-args see-job-id)
        handler (partial
                  comm-framework/wrap-handle-msg
                  sample-scheduler/handle-msg)]
    (log/trace "Got handler:" handler)
    (log/trace "Using initial state:" state)
    (log/debug "Starting example model scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over example model scheduler channel messages ...")
    (async/reduce handler state ch)
    (scheduler/join! driver)))
