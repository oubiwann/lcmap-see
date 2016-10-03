(ns lcmap.see.backend.native.models.sample-pipe
  "This sample runner demonstrates kicking off a job that executes a series
  of piped commands on the system local to the LCMAP REST server, capturing
  standard out."
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.util :as util]))

(defn exec-pipe-run
  ""
  [[job-id line-number unique-count bytes words lines]]
  (let [number-flag (util/make-flag "--number" line-number :unary? true)
        count-flag (util/make-flag "--count" unique-count :unary? true)
        bytes-flag (util/make-flag "--bytes" bytes :unary? true)
        words-flag (util/make-flag "--words" words :unary? true)
        lines-flag (util/make-flag "--lines" lines :unary? true)
        cmd1 (remove nil? ["/bin/cat" number-flag "/etc/hosts"])
        cmd2 (remove nil? ["/usr/bin/uniq" count-flag])
        cmd3 (remove nil? ["/usr/bin/wc" bytes-flag words-flag lines-flag])
        promises (exec/sh-pipe cmd1 cmd2 cmd3)]
    (log/debug "Preparing piped commands: %s"
      (string/join " | " [(string/join " " cmd1)
                          (string/join " " cmd2)
                          (string/join " " cmd3)]))
    (let [result (map deref promises)
          all-zero-exit? (every? zero? (map :exit result))
          all-no-errors? (every? nil? (map :err result))]
      (if (and all-zero-exit? all-no-errors?)
        (:out (last result))
        [:error "unexpected output" result]))))

(defn run-model
  "This sample model runs a series of unix commands (one piped to the next) in
  order to demonstrate how models can be configured that have this requirement.

  In the particular case of this sample model, the following Linux shell
  utilities pipe output to each other:

  * ``cat /etc/hosts`` (with the optional ``--number`` flag)
  * ``uniq`` (with the optional ``--count`` flag)
  * ``wc`` (with the optional ``--bytes``, ``--words``, or ``--lines`` flags)"
  [backend-impl model-name line-number unique-count bytes words lines]
  (let [cfg (:cfg backend-impl)
        tracker-impl (tracker/new model-name backend-impl)
        model-func #'exec-pipe-run
        model-args [job-id line-number unique-count bytes words lines]]
    (log/debugf "run-model has [func args]: [%s %s]" model-func model-args)
    (tracker/track-job
      tracker-impl
      model-func
      model-args)))
