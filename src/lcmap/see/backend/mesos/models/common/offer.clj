(ns lcmap.see.backend.mesos.models.common.offer
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]))

(defn get-agent-id
  ""
  [offer]
  (log/debug "Got offer:" offer)
  (:agent-id offer))
