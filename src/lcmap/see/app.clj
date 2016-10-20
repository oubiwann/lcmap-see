(ns lcmap.see.app
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clojusc.twig :as logger]
            [lcmap.see.backend.mesos.models.sample.executor :as sample-ex]
            [lcmap.see.components :as components]
            [lcmap.see.util :as util])
  (:gen-class))

(defn usage
  ""
  []
  (println)
  (-> 'lcmap.see.app
      (util/get-docstring '-main)
      (println)))

(defn -main
  "This function serves two purposes:
   1. A primary entrypoint for the LCMAP SEE codebase
   2. The entrypoint for the LCMAP SEE command line tool.

  It is expected that this function be called from ``lein`` in one of the
  two following manners:

  ```
  $ lein see 127.0.0.1:5050 <science-model> <task-type>
  ```

  where ``<science-model>`` is the name for a science model supported
  by the Mesos backend and where ``<task-type>`` is one of:

  * ``executor``
  * ``framework``

  That being said, only a framework should call the command line interface
  with the ``executor`` task type; as a user, you will almost certainly
  only call with the ``framework`` task type.

  Note that in order for this to work, one needs to add the following alias to
  the project's ``project.clj``:

  ```clj
  :aliases {\"see\" [\"run\" \"-m\" \"lcmap.see.app\"]}
  ```"
  ([]
    (logger/set-level! ['lcmap] :info)
    (let [system (components/init)]
      (log/info
        "LCMAP SEE service's local IP address:"
        (util/get-local-ip))
      (component/start system)
      (util/add-shutdown-handler #(component/stop system))))
  ([flag]
    (case flag
      "--help" (usage)
      "-h" (usage)))
  ([master science-model task-type]
    (let [system (components/init)]
      (component/start system)
      (util/add-shutdown-handler #(component/stop system))
      (log/debug "Using master:" master)
      (log/debug "Got science-model:" science-model)
      (log/debug "Got task-type:" task-type)
      (condp = task-type
        ;; XXX no "framework" cli support yet ...
        ;;"framework" (sample-fr/run)
        ;; XXX in the future, this can be made dynamic -- for now, we just want
        ;; to be able to execute the sample model's executor for testing
        ;; purposes
        "executor" (sample-ex/run master))
      (util/finish 0))))
