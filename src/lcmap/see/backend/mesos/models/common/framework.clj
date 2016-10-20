(ns lcmap.see.backend.mesos.models.common.framework
  "General framework functions for Mesomatic-based models."
  (:require [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [lcmap.see.util :as util]
            [mesomatic.scheduler :as scheduler]))

(defn wrap-handle-msg
  "Wrap the handle-msg multi-method so that exceptions can be properly caught
  and the scheduler can be given the change to perform an abort procedure.

  Anticipated use of the function is:

    (async.core/reduce (partial wrap-handle-msg handle-msg) { ... } ch)

  For use with the Mesomatic async scheduler API."
  [handler state payload]
  (try
    (handler state payload)
    (catch Exception e
      (log/error "Got error:" (.getMessage e))
      (log/debug "Error details: " e)
      (log/debugf "Passed args:\nHandler: %s\nState: %s\nPayload: %s"
                  handler (pprint state) (pprint payload))
      (scheduler/abort! (:driver state))
      (reduced
        (assoc state :error e)))))
