(ns lcmap.see.backend.mesos.models.sample-docker.scheduler
  "This is the namespace that drives much of the activity for the framework.
  It implements the callbacks specificed by the async Mesomatic scheduler
  dirver.

  Note that these are not callbacks in the node.js or even Twisted (Python)
  sense of the word; they are like Erlang OTP callbacks. For more
  information on the distinguishing characteristics, take a look at Joe
  Armstrong's blog post on Red/Green Callbacks:
   * http://joearms.github.io/2013/04/02/Red-and-Green-Callbacks.html"
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler]
            [mesomatic.types :as types]
            [lcmap.see.backend.mesos.models.common.scheduler :as comm-scheduler]
            [lcmap.see.backend.mesos.models.common.payload :as comm-payload]
            [lcmap.see.backend.mesos.models.common.resources :as comm-resources]
            [lcmap.see.backend.mesos.models.common.state :as comm-state]
            [lcmap.see.backend.mesos.util :as util]
            [lcmap.see.util :as see-util]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Data & Setup
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn docker-info
  [docker-tag]
  {:image docker-tag
   :network :docker-network-bridge})

(defn container-info
  [docker-tag]
  {:type :container-type-docker
   :docker (docker-info docker-tag)})

(defn task-info
  [task-id agent-id docker-tag]
  {:task-id task-id
   :name (format "Docker Task %s" task-id)
   :slave-id agent-id
   :agent-id agent-id
   :container (container-info docker-tag)
   :command {:shell false}
   :resources [{:name "cpus"
                :type :value-scalar
                :scalar 1.0}
               {:name "mem"
                :type :value-scalar
                :scalar (* 1024 30)}]})

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework callbacks
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defmulti handle-msg
  "This is a custom multimethod for handling messages that are received on the
  async scheduler channel.

  Note that:

  * though the methods are associated with types whose names match the
    scheduler API, these functions and those are quite different and do not
    accept the same parameters
  * each handler's callback (below) only takes two parameters:
     1. state that gets passed to successive calls (if returned by the handler)
     2. the payload that is sent to the async channel by the scheduler API
  * as such, if there is something in a message which you would like to persist
    or have access to in other functions, you'll need to assoc it to state."
  (comp :type last vector))

(defmethod handle-msg :registered
  [state payload]
  (let [master-info (comm-payload/get-master-info payload)
        framework-id (comm-payload/get-framework-id payload)]
    (assoc state :master-info master-info
                 :framework-id {:value framework-id})))

(defmethod handle-msg :disconnected
  [state payload]
  (log/infof "Framework %s disconnected."
             (comm-payload/get-framework-id payload))
  state)

(defmethod handle-msg :resource-offers
  [state payload]
  (log/info "Handling :resource-offers message ...")
  (let [offer-data (first (comm-payload/get-offers payload))
        offer-id (:id offer-data)
        slave-id (:slave-id offer-data)
        ;agent-id (:agent-id offer-data)
        task-id (util/get-uuid)
        task (task-info task-id slave-id (:docker-tag state))
        driver (comm-state/get-driver state)]
    (if-not (:offer? state)
      (do
        (log/trace "Got state:" (pprint state))
        (log/trace "Got offer data:" offer-data)
        (log/trace "Offer slave-id:" slave-id)
        ;(log/trace "Offer agent-id:" agent-id)
        (log/trace "Got payload for task:" (pprint task))
        (log/trace "Driver:" driver)
        (log/info "Launching tasks ...")
        ;; New API:
        ; (scheduler/accept-offers
        ;   driver
        ;   [offer-id]
        ;   [{:type :operation-launch
        ;     :tasks [task]}])
        ;; Deprecated API:
        (scheduler/launch-tasks! driver offer-id [task] {:refuse-seconds 1})
        (assoc state :offer? true :tasks [task]))
      (do
        (log/info "Already accepted offer and created task; ignoring offer.")
        state))))

(defmethod handle-msg :status-update
  [state payload]
  (log/trace "Got status-update state:" (pprint state))
  (log/trace "Got status-update payload:" (pprint payload))
  (let [status (:status payload)
        state-name (:state status)]
    (log/infof "Handling :status-update message with state '%s' ..."
               state-name)
    (case state-name
      :task-staging (do (log/info "Task is staging ...")
                        (log/debug "Raw data:" (:data status))
                        (log/debug "Data:" (.toStringUtf8 (:data status)))
                        state)
      :task-starting (do (log/info "Task is starting ...")
                         (log/debug "Raw data:" (:data status))
                         (log/debug "Data:" (.toStringUtf8 (:data status)))
                         state)
      :task-running (do (log/info "Task is running ...")
                        (log/debug "Raw data:" (:data status))
                        (log/debug "Data:" (.toStringUtf8 (:data status)))
                        state)
      :task-finished (do (log/info "Task finished.")
                         (log/debug "Raw data:" (:data status))
                         (log/debug "Data:" (.toStringUtf8 (:data status)))
                         (scheduler/stop! (:driver state))
                         state)
      :task-failed (do (log/error "Task failed.")
                       (scheduler/stop! (:driver state))
                       state)
      :task-killed (do (log/error "Task killed.")
                        (scheduler/stop! (:driver state))
                        state)
      :task-lost (do (log/error "Task lost.")
                     (scheduler/stop! (:driver state))
                     state)
      :task-error (do (log/error "Task error.")
                      (scheduler/stop! (:driver state))
                      state)
      (do (log/debugf "Got unexpected status: '%s'" state-name)
          (log/debug "Raw data:" (:data status))
          (log/debug "Data:" (.toStringUtf8 (:data status)))
          (scheduler/stop! (:driver state))
          state))))

(defmethod handle-msg :disconnected
  [state payload]
  (log/infof "Framework %s disconnected."
             (comm-payload/get-framework-id payload))
  state)

(defmethod handle-msg :offer-rescinded
  [state payload]
  (let [framework-id (comm-payload/get-framework-id payload)
        offer-id (comm-payload/get-offer-id payload)]
    (log/infof "Offer %s rescinded from framework %s." offer-id framework-id)
    state))

(defmethod handle-msg :framework-message
  [state payload]
  (let [framework-id (comm-payload/get-framework-id payload)
        executor-id (comm-payload/get-executor-id payload)
        slave-id (comm-payload/get-slave-id payload)]
    (comm-payload/log-framework-msg framework-id executor-id slave-id payload)
    state))

(defmethod handle-msg :slave-lost
  [state payload]
  (let [slave-id (comm-payload/get-slave-id payload)]
    (log/error "Framework %s lost connection with slave %s."
               (comm-payload/get-framework-id payload)
               slave-id)
    state))

(defmethod handle-msg :executor-lost
  [state payload]
  (let [executor-id (comm-payload/get-executor-id payload)
        slave-id (comm-payload/get-slave-id payload)
        status (comm-payload/get-status payload)]
    (log/errorf (str "Framework lost connection with executor %s (slave=%s) "
                     "with status code %s.")
                executor-id slave-id status)
    state))

(defmethod handle-msg :error
  [state payload]
  (let [message (comm-payload/get-message payload)]
    (log/error "Got error message: " message)
    (log/debug "Data:" (pprint payload))
    state))

(defmethod handle-msg :default
  [state payload]
  (log/warn "Unhandled message: " (pprint payload))
  state)
