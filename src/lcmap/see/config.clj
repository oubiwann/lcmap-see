(ns lcmap.see.config
  (:require [lcmap.config.helpers :as cfg]
            [schema.core :as schema]))

(def opt-spec [])

(def db-cfg-schema
  {:hosts [schema/Str]
   :user schema/Str
   :pass schema/Str
   :job-namespace schema/Str
   :job-table schema/Str})

(def msg-cfg-schema
  {:host schema/Str})

(def cfg-schema
  {:lcmap.see.components.db db-cfg-schema
   :lcmap.see.components.messaging msg-cfg-schema
   schema/Keyword schema/Any})

(def defaults
  {:ini (clojure.java.io/file (System/getenv "HOME") ".usgs" "lcmap.ini")
   :spec opt-spec
   :args *command-line-args*
   :schema cfg-schema})
