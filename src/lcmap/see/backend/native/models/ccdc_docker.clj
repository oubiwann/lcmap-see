(ns lcmap.see.backend.native.models.ccdc-docker
  "This is the job runner for the CCDC model that is run in a docker container.
  Ideally, this runner will be used by an instance of the LCMAP REST server
  that is *not* running in Docker, but instead being run (deployed) on a
  non-virtualized operating system."
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as tracker]
            [lcmap.see.util :as util])
  (:import [java.io ByteArrayOutputStream]))

(def dockerhub-org "usgseros")
(def dockerhub-repo "debian-c-ccdc")
(def docker-tag (format "%s/%s" dockerhub-org dockerhub-repo))

(defn exec-docker-run
  "This is the function that actually calls the science model. This model
  accomplishes this by calling the ccdc Docker image. This is essentially a
  pass-through to the ccdc executable in the Docker image, so the Docker ``run``
  command takes all the same parameters/flags that would be given if the ccdc
  executable was getting called directly."
  [[job-id row col in-dir out-dir scene-list verbose]]
  (let [verbose-flag (util/make-flag "--verbose" verbose :unary? true)
        in-dir-flag (util/make-flag "--inDir" in-dir)
        out-dir-flag (util/make-flag "--outDir" out-dir)
        row-flag (util/make-flag "--row" row)
        col-flag (util/make-flag "--col" col)
        scene-list-flag (util/make-flag "--sceneList" scene-list)
        cmd (remove nil? ["/usr/bin/sudo"
                          "/usr/bin/docker"
                          "run" "-t" docker-tag
                          row-flag col-flag in-dir-flag out-dir-flag
                          scene-list-flag verbose-flag])
        result @(exec/sh cmd)]
    (case (:exit result)
      0 (:out result)
      1 (:err result)
      [:error "unexpected output" result])))

(defn run-model [component job-id default-row result-table
                 row col in-dir out-dir scene-list verbose]
  ;; Define some vars for pedagogical clarity
  (let [backend (get-in component [:see :backend :name])
        tracker-impl (get-in component [:see :job :tracker])
        func #'exec-docker-run
        args [job-id row col in-dir out-dir scene-list verbose]]
    (log/debugf "run-model has [func args]: [%s %s]" func args)
    (tracker/track-job
      tracker-impl
      job-id
      default-row
      result-table
      [func args])))
