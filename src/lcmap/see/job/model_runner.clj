(ns lcmap.see.job.model-runner
  (:require [clojure.tools.logging :as log]
            [lcmap.see.job.tracker :as jt]))

(defn model-func []
    :noop)

(defn get-model-hash [args]
  :fix-me!)

(defn run-model []
  (jt/track-job #'model-func))
