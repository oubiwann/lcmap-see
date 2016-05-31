(ns ^{:doc
  ""}
  lcmap.see.model.ndvi
  (:require [clojure.tools.logging :as log]
            [clj-commons-exec :as exec]
            [lcmap.see.job.tracker :as jt]))

(def dockerhub-org "usgseros")
(def dockerhub-repo "lcmap-model-wrapper")
(def docker-tag (format "%s/%s" dockerhub-org dockerhub-repo))

(defn command
  ""
  [job-id x y t1 t2]
  ["/usr/bin/docker" "run" "-t" docker-tag
   "python3" "index/cli.py" "ndvi"
   "--job-id" job-id
   "--x" x
   "--y" y
   "--t1" t1
   "--t2" t2])

(defn execute
  ""
  [job-id x y t1 t2]
  (let [cmd (command job-id x y t1 t2)
        result @(exec/sh cmd)]
    (log/debugf "exec '%s' results: %s" "ndvi model" result)
    (case (:exit result)
      0 (:out result)
      1 (:err result)
      [:error "unexpected output" result])))

(defn before
  ""
  [])

(defn after
  ""
  [])

(defn run
  ""
  [job-id x y t1 t2]
  ;; before ... track job inputs
  ;; execute
  ;; after  ... track job outputs
  (execute job-id x y t1 t2))


#_(defn run-model [job-id x y t1 t2]
  (let [func #'execute
        args [job-id x y t1 t2]]
    (log/debugf "run-model has [func args]: [%s %s]" func args)
    (jt/track-job component
                  job-id
                  default-row
                  result-table
                  [func args])))
