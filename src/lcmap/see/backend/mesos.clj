(ns lcmap.see.backend.mesos
  (:require [clojure.core.async :as async :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lcmap.see.backend.core :refer [ExecutionBackend]]
            [lcmap.see.util :as util]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]])
  (:import java.util.UUID))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; LCMAP SEE backend (interface) implementation for Mesos
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; The following record is designed to be used with the ExecutionBackend
;;; protocol in order to create a long-running data structure that holds
;;; all needed information for any Mesos framework to start up, run executors
;;; and/or tasks, and properly manage itself.

(defrecord MesosBackend [name cfg]
  ExecutionBackend
  (set-up [this] this)
  (tear-down [this] this))

(defn new-backend
  ""
  [cfg]
  (->MesosBackend :native cfg))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; LCMAP SEE Mesos utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn get-uuid
  "A Mesos/protobufs-friendly UUID wrapper."
  []
  (->> (UUID/randomUUID)
       (str)
       (assoc {} :value)))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Mesos payload utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; These are intended to make the callbacks below easier to read, while
;;; providing a little buffer around data and implementation: if (when) the
;;; Mesos messaging API/data structure changes (again), only the functions
;;; below will need to be changed (you won't have to dig through the rest of
;;; the code looking for data structures to update).

(defn get-framework-id
  ""
  [payload]
  (get-in payload [:framework-id :value]))

(defn get-offers
  ""
  [payload]
  (get-in payload [:offers]))

(defn get-error-msg
  ""
  [payload]
  (let [msg (get-in payload [:status :message])]
    (cond
      (empty? msg) (name (get-in payload [:status :reason]))
      :true msg)))

(defn get-master-info
  ""
  [payload]
  (:master-info payload))

(defn get-offer-id
  ""
  [payload]
  (:offer-id payload))

(defn get-status
  ""
  [payload]
  (:status payload))

(defn get-state
  ""
  [payload]
  (name (get-in payload [:status :state])))

(defn healthy?
  ""
  [payload]
  (get-in payload [:status :healthy]))

(defn get-executor-id
  ""
  [payload]
  (get-in payload [:executor-id :value]))

(defn get-slave-id
  ""
  [payload]
  (get-in payload [:slave-id :value]))

(defn get-message
  ""
  [payload]
  (:message payload))

(defn get-bytes
  ""
  [payload]
  (.toStringUtf8 (get-in payload [:status :data])))

(defn log-framework-msg
  ""
  [framework-id executor-id slave-id payload]
  (let [bytes (String. (:data payload))
        log-type? (partial string/includes? bytes)]
    (cond
      (log-type? "TRACE") (log/trace bytes)
      (log-type? "DEBUG") (log/debug bytes)
      (log-type? "INFO") (log/info bytes)
      (log-type? "WARN") (log/warn bytes)
      (log-type? "ERROR") (log/error bytes)
      :else (log/infof
              "Framework %s got message from executor %s (slave=%s): %s"
              framework-id executor-id slave-id bytes))))

(defn get-task-state
  ""
  [payload]
  (get-in payload [:status :state]))

(defn get-task-id
  ""
  [payload]
  (get-in payload [:status :task-id :value]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Mesos state utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; These are intended to make the callbacks below easier to read, while
;;; providing a little buffer around data and implementation of our own state
;;; data structure.

(defn get-driver
  ""
  [state]
  (:driver state))

(defn get-channel
  ""
  [state]
  (:channel state))

(defn get-exec-info
  ""
  [state]
  (:exec-info state))

(defn get-max-tasks
  ""
  [state]
  (get-in state [:limits :max-tasks]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; General utility functions for Mesomatic frameworks
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn do-unhealthy-status
  ""
  [state-name state payload]
  (log/debug "Doing unhealthy check ...")
  (do
    (log/errorf "%s - %s"
                state-name
                (get-error-msg payload))
    (async/close! (get-channel state))
    (scheduler/stop! (get-driver state))
    (util/finish :exit-code 127)
    state))

(defn check-task-finished
  ""
  [state payload]
  (if (= (get-task-state payload) :task-finished)
    (let [task-count (inc (:launched-tasks state))
          new-state (assoc state :launched-tasks task-count)]
      (log/debug "Incremented task-count:" task-count)
      (log/info "Tasks finished:" task-count)
      (if (>= task-count (get-max-tasks state))
        (do
          (scheduler/stop! (get-driver state))
          (util/finish :exit-code 0)
          new-state)
        new-state))
    state))

(defn check-task-abort
  ""
  [state payload]
  (if (or (= (get-task-state payload) :task-lost)
          (= (get-task-state payload) :task-killed)
          (= (get-task-state payload) :task-failed))
    (let [status (:status payload)]
      (log/errorf (str "Aborting because task %s is in unexpected state %s "
                       "with reason %s from source %s with message '%s'")
                  (get-task-id payload)
                  (:state status)
                  (:reason status)
                  (:source status)
                  (:message status))
      (scheduler/abort! (get-driver state))
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
