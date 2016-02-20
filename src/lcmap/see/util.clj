(ns lcmap.see.util
  (:require [clojure.core.memoize :as memo]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [digest]))

(defn get-args-hash [model-name & args]
  (->> args
       (serialize)
       (str model-name)
       (digest/md5)))

(defn add-shutdown-handler [func]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. func)))

(defn in?
  "This function returns true if the provided seqenuce contains the given
  elment."
  [seq elm]
  (some #(= elm %) seq))

(defn make-bool
  ""
  [input]
  (case input
    0 false
    "0" false
    false false
    "false" false
    :false false
    nil false
    "nil" false
    :nil false
    true))

(defn make-flag
  "There are three cases we want to handle for command line options:
  * a flag that takes a value
  * a flag which should be passed, since a value was given
  * a flag which should not be passed, since no value was given"
  [flag value & {:keys [unary?] :or {unary? false}}]
  (cond
    unary? (if (make-bool value)
             flag
             nil)
    (nil? value) nil
    :else (format "%s %s" flag value)))
