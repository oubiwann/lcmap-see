(ns lcmap.see.backend.mesos.models.common.payload
  "These are intended to make the callbacks below easier to read, while
  providing a little buffer around data and implementation: if (when) the
  Mesos messaging API/data structure changes (again), only the functions
  below will need to be changed (you won't have to dig through the rest of
  the code looking for data structures to update)."
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]))

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
  (log/debug "Getting master info ...")
  (log/debug "Got payload:" payload)
  (log/debug "Returning master-info:" (:master-info payload))
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

(defn get-agent-id
  ""
  [payload]
  (get-in payload [:agent-id :value]))

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
