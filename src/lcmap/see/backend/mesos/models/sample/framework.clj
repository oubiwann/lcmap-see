(ns lcmap.see.backend.mesos.models.sample.framework
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
            [mesomatic.types :as types]
            [lcmap.see.backend.mesos.models.common.framework :as comm-framework]
            [lcmap.see.backend.mesos.models.common.payload :as comm-payload]
            [lcmap.see.backend.mesos.models.common.resource :as comm-resource]
            [lcmap.see.backend.mesos.models.common.state :as comm-state]
            [lcmap.see.backend.mesos.models.sample.executor :as executor]
            [lcmap.see.backend.mesos.models.sample.offers :as offers]
            [lcmap.see.backend.mesos.models.sample.task :as task]
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

(def framework-info-map {:name "Example Framework (Clojure)"
                         :principal "test-framework-clojure"
                         :checkpoint true})
(def limits
  "Note that :max-tasks gets set via an argument passed to the `run` function."
  {:cpus-per-task 1
   :mem-per-task 128
   :max-tasks nil})

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework callbacks
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; Note that these are not callbacks in the node.js or even Twisted (Python)
;;; sense of the word; they are like Erlang OTP callbacks. For more
;;; information on the distinguishing characteristics, take a look at Joe
;;; Armstrong's blog post on Red/Green Callbacks:
;;;  * http://joearms.github.io/2013/04/02/Red-and-Green-Callbacks.html

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
        framework-id (comm-payload/get-framework-id payload)
        exec-info (executor/cmd-info-map
                    master-info framework-id (util/cwd))]
    (log/info "Registered with framework id:" framework-id)
    (log/trace "Got master info:" (pprint master-info))
    (log/trace "Got state:" (pprint state))
    (log/trace "Got exec info:" (pprint exec-info))
    (assoc state :exec-info exec-info
                 :master-info master-info
                 :framework-id {:value framework-id})))

(defmethod handle-msg :disconnected
  [state payload]
  (log/infof "Framework %s disconnected." (comm-payload/get-framework-id payload))
  state)

(defmethod handle-msg :resource-offers
  [state payload]
  (log/info "Handling :resource-offers message ...")
  (log/trace "Got state:" (pprint state))
  (let [offers-data (comm-payload/get-offers payload)
        offer-ids (offers/get-ids offers-data)
        tasks (offers/process-all state payload offers-data)
        driver (comm-state/get-driver state)]
    (log/trace "Got offers data:" offers-data)
    (log/trace "Got offer IDs:" (map :value offer-ids))
    (log/trace "Got other payload:" (pprint (dissoc payload :offers)))
    (log/debug "Created tasks:"
               (string/join ", " (map task/get-pb-name tasks)))
    (log/tracef "Got payload for %d task(s): %s"
                (count tasks)
                (pprint (into [] (map pprint tasks))))
    (log/info "Launching tasks ...")
    (scheduler/accept-offers
      driver
      offer-ids
      [{:type :operation-launch
        :tasks tasks}])
    (assoc state :offers offers-data :tasks tasks)))

(defmethod handle-msg :status-update
  [state payload]
  (let [status (comm-payload/get-status payload)
        state-name (comm-payload/get-state payload)]
    (log/infof "Handling :status-update message with state '%s' ..."
               state-name)
    (log/trace "Got state:" (pprint state))
    (log/trace "Got status:" (pprint status))
    (log/trace "Got status info:" (pprint payload))
    (scheduler/acknowledge-status-update (comm-state/get-driver state) status)
    (if-not (comm-payload/healthy? payload)
      (comm-framework/do-unhealthy-status state-name state payload)
      (comm-framework/do-healthy-status state payload))))

(defmethod handle-msg :disconnected
  [state payload]
  (log/infof "Framework %s disconnected." (comm-payload/get-framework-id payload))
  state)

(defmethod handle-msg :offer-rescinded
  [state payload]
  (let [framework-id (comm-state/get-framework-id state)
        offer-id (comm-payload/get-offer-id payload)]
    (log/infof "Offer %s rescinded from framework %s."
               offer-id (comm-payload/get-framework-id payload))
    state))

(defmethod handle-msg :framework-message
  [state payload]
  (let [framework-id (comm-state/get-framework-id state)
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

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  "This is the function that actually runs the framework."
  [component]
  (log/info "Running LCMAP SEE sample Mesos framework ...")
  (log/debug "Got component:" component)
  (let [master (util/get-master component)
        ch (chan)
        sched (async-scheduler/scheduler ch)
        driver (scheduler-driver sched
                                 framework-info-map
                                 master
                                 nil
                                 false)]
    (log/debug "Starting sample scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over sample scheduler channel messages ...")
    (a/reduce
      (partial comm-framework/wrap-handle-msg handle-msg)
      {:driver driver
       :channel ch
       :exec-info nil
       :launched-tasks 0
       :limits (assoc limits :max-tasks task-count)
       :cfg component}
      ch)
    (scheduler/join! driver)))
