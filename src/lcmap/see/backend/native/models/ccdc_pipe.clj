(ns lcmap.see.backend.native.models.ccdc-pipe
  "This runner allows the CCDC mode to be run on the same server as the LCMAP
  REST service, piping results from the 'lcmap query rod' command (with
  several command line options) to the 'ccdc' executable (also with several
  command line options)."
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.job.tracker.native]
            [lcmap.see.util :as util]))

(defn exec-pipe-run
  "This is the function that actually calls the science model. This model
  accomplishes this in two stages:

  1) it calls to the lcmap command line tool, querying for rod data
  2) it pipes these results to the ccdc executable

  In order for this to work, the Python environment for lcmap needs to
  have been setup (requirements installed) and set to active. Also, the
  ccdc needs to have been compiled and installed to a location that is
  on the system PATH."
  [[job-id spectra x-val y-val start-time end-time
    row col in-dir out-dir scene-list verbose]]
  (let [;; lcmap cmdline tool flags
        spectra-flag (util/make-flag "--spectra" spectra)
        x-flag (util/make-flag "-x" x-val)
        y-flag (util/make-flag "-y" y-val)
        start-flag (util/make-flag "--t1" start-time)
        end-flag (util/make-flag "--t1" end-time)
        ;; ccdc cmdline flags
        verbose-flag (util/make-flag "--verbose" verbose :unary? true)
        in-dir-flag (util/make-flag "--inDir" in-dir)
        out-dir-flag (util/make-flag "--outDir" out-dir)
        row-flag (util/make-flag "--row" row)
        col-flag (util/make-flag "--col" col)
        scene-list-flag (util/make-flag "--sceneList" scene-list)
        cmd1 (remove nil? ["lcmap" "query" "rod" spectra-flag x-flag y-flag
                           start-flag end-flag])
        cmd2 (remove nil? ["ccdc" row-flag col-flag in-dir-flag out-dir-flag
                           scene-list-flag verbose-flag])
        promises (exec/sh-pipe cmd1 cmd2)]
    (log/debug "Preparing piped commands: %s"
      (string/join " | " [(string/join " " cmd1)
                          (string/join " " cmd2)]))
    (let [result (map deref promises)
          all-zero-exit? (every? zero? (map :exit result))
          all-no-errors? (every? nil? (map :err result))]
      (if (and all-zero-exit? all-no-errors?)
        (:out (last result))
        [:error "unexpected output" result]))))

(defn run-model
  "This a prototype CCDC model which runs the lcmap command line tool's 'rod' query
  and pipes the results to ccdc as input."
  [component job-id default-row result-table spectra x-val y-val
   start-time end-time row col in-dir out-dir scene-list verbose]
  ;; Define some vars for pedagogical clarity
  (let [backend (get-in component [:see :backend :name])
        tracker-impl (get-in component [:see :job :tracker])
        func #'exec-pipe-run
        args [job-id spectra x-val y-val start-time end-time
                     row col in-dir out-dir scene-list verbose]]
    (log/debugf "run-model has [func args]: [%s %s]" func args)
    (tracker/track-job
      tracker-impl
      job-id
      default-row
      result-table
      [func args])))
