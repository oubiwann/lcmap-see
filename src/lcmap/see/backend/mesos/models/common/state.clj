(ns lcmap.see.backend.mesos.models.common.state
  "These are intended to make callbacks easier to read, while providing a
  little abstraction buffer around data and implementation of our own state
  data structure."
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]))

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
