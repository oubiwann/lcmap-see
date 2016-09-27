(ns lcmap.see.backend.mesos.models.sample.executor
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.executor :as executor :refer [executor-driver]]
            [mesomatic.types :as types]
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

(defn info-map
  ""
  []
  {:executor-id (util/get-uuid)
   :name "LCMAP SEE Sample Executor (Clojure)"})

(defn cmd-info-map
  ""
  [master-info framework-id]
  (into
    (info-map)
    {:framework-id {:value framework-id}
     :command {:value (format "")
               :shell true}}))

(defn get-executor-id
  ""
  [payload]
  (get-in payload [:executor-info :executor-id :value]))

(defn get-task-id
  ""
  [payload]
  (get-in payload [:task :task-id :value]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn send-log
  ""
  [state level message]
  (let [str-level (string/upper-case (name level))
        msg (format "%s - %s" str-level message)
        bytes (.getBytes (str "Message from sample executor: " msg))]
    (if (= level :debug)
      (log/debugf "Sending message to sample framework: %s ..." msg))
    (executor/send-framework-message! (:driver state) bytes)))

(defn send-log-trace
  ""
  [state message]
  (send-log state :trace message))

(defn send-log-debug
  ""
  [state message]
  (send-log state :debug message))

(defn send-log-info
  ""
  [state message]
  (send-log state :info message))

(defn send-log-warn
  ""
  [state message]
  (send-log state :warn message))

(defn send-log-error
  ""
  [state message]
  (send-log state :error message))

(defn update-task-success
  ""
  [task-id state payload]
  (let [executor-id (get-executor-id payload)
        driver (:driver state)]
    (executor/send-status-update!
      driver
      (task/status-running executor-id task-id))
    (send-log-info state (str "Running sample task " task-id))

    ;; This is where one would perform the requested task:
    ;; ...
    (Thread/sleep (rand-int 500))
    ;; ...
    ;; Task complete.

    (executor/send-status-update!
      driver
      (task/status-finished executor-id task-id))
    (send-log-info state (str "Finished sample task " task-id))))

(defn update-task-fail
  ""
  [task-id e state payload]
  (let [executor-id (get-executor-id payload)]
    (send-log-error state (format "Got exception for sample task %s: %s"
                                  task-id (pprint e)))
    (executor/send-status-update!
      (:driver state)
      (task/status-failed executor-id task-id))
    (send-log-info state (format "Sample task %s failed" task-id))))

(defn run-task
  ""
  [task-id state payload]
  (try
    (update-task-success task-id state payload)
    (catch Exception e
      (update-task-fail task-id e state payload))))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework callbacks
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; Note that these are not callbacks in the node.js or even Twisted (Python)
;;; sense of the word; they are like Erlang OTP callbacks. For more
;;; information on the distinguishing characteristics, take a look at Joe
;;; Armstrong's blog post on Red/Green Callbacks:
;;;  * http://joearms.github.io/2013/04/02/Red-and-Green-Callbacks.html

(defmulti handle-msg (comp :type last vector))

(defmethod handle-msg :registered
  [state payload]
  (send-log-info state (str "Registered sample executor: " (get-executor-id payload)))
  state)

(defmethod handle-msg :reregistered
  [state payload]
  (send-log-info state
                 (str "Reregistered sample executor: " (get-executor-id payload)))
  state)

(defmethod handle-msg :disconnected
  [state payload]
  (send-log-info state (str "Sample executor has disconnected: " (pprint payload)))
  state)

(defmethod handle-msg :launch-task
  [state payload]
  (let [task-id (get-task-id payload)]
    (send-log-info state (format "Launching sample task %s ..." task-id))
    (log/debug "Sample task id:" task-id)
    (send-log-trace state (str "Sample task payload: " (pprint payload)))
    (-> (run-task task-id state payload)
        (Thread.)
        (.start))
    state))

(defmethod handle-msg :kill-task
  [state payload]
  (send-log-info state (str "Killing sample task: " (pprint payload)))
  state)

(defmethod handle-msg :framework-message
  [state payload]
  (send-log-info state (str "Got sample framework message: " (pprint payload)))
  state)

(defmethod handle-msg :shutdown
  [state payload]
  (send-log-info state (str "Shutting down sample executor: " (pprint payload)))
  state)

(defmethod handle-msg :error
  [state payload]
  (send-log-error state (str "Error in sample executor: " (pprint payload)))
  state)

(defmethod handle-msg :default
  [state payload]
  (send-log-warn (str "Unhandled sample executor message: " (pprint payload)))
  state)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Executor entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  ""
  [master]
  (log/infof "Running sample executor ...")
  (let [ch (chan)
        exec (async-executor/executor ch)
        driver (executor-driver exec)]
    (log/debug "Starting sample executor ...")
    (executor/start! driver)
    (log/debug "Reducing over sample executor channel messages ...")
    (a/reduce handle-msg {:driver driver
                          :ch ch} ch)
    (executor/join! driver)))
