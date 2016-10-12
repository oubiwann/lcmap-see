(ns lcmap.see.backend.mesos.models.common.scheduler
  "General framework functions for Mesomatic-based models."
  (:require [clojure.core.async :as async :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [lcmap.see.backend :as see]
            [lcmap.see.backend.mesos.models.common.payload :as comm-payload]
            [lcmap.see.backend.mesos.models.common.state :as comm-state]
            [lcmap.see.util :as util]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]))

(defn do-unhealthy-status
  ""
  [state-name state payload]
  (log/debug "Doing unhealthy check ...")
  (do
    (log/errorf "%s - %s"
                state-name
                (comm-payload/get-error-msg payload))
    (async/close! (comm-state/get-channel state))
    (scheduler/stop! (comm-state/get-driver state))
    (util/finish :exit-code 127)
    state))

(defn check-task-finished
  ""
  [state payload]
  (if (= (comm-payload/get-task-state payload) :task-finished)
    (let [task-count (inc (:launched-tasks state))
          new-state (assoc state :launched-tasks task-count)
          tracker-impl (get-in state [:backend :job :tracker])
          tracker-args {:type :job-finish-run}]
      (log/debug "Incremented task-count:" task-count)
      (log/debug "Got tracker implementation:" tracker-impl)
      (log/info "Tasks finished:" task-count)
      (if (>= task-count (comm-state/get-max-tasks new-state))
        (do
          (log/debug "Preparing to stop scheduler ...")
          (scheduler/stop! (comm-state/get-driver new-state))
          ;; XXX get task output
          ;; XXX send output to job tracker using tracker.base/send-msg
          (util/finish :exit-code 0)
          new-state)
        new-state))
    state))

(defn check-task-abort
  ""
  [state payload]
  (if (or (= (comm-payload/get-task-state payload) :task-lost)
          (= (comm-payload/get-task-state payload) :task-killed)
          (= (comm-payload/get-task-state payload) :task-failed))
    (let [status (:status payload)]
      (log/errorf (str "Aborting because task %s is in unexpected state %s "
                       "with reason %s from source %s with message '%s'")
                  (comm-payload/get-task-id payload)
                  (:state status)
                  (:reason status)
                  (:source status)
                  (:message status))
      (scheduler/abort! (comm-state/get-driver state))
      (util/finish :exit-code 127)
      state)
    state))

(defn do-healthy-status
  ""
  [state payload]
  (log/debug "Doing healthy check ...")
  (-> state
      (check-task-finished payload)
      (check-task-abort payload)))
