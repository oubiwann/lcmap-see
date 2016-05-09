(ns lcmap.see.config
  (:require [lcmap.config.helpers :refer :all]
            [schema.core :as schema]))

(def opt-spec [])

(def db-cfg-schema
  {:db-hosts [schema/Str]
   :db-user schema/Str
   :db-pass schema/Str
   :job-namespace schema/Str
   :job-table schema/Str})

(def msg-cfg-schema
  {:msg-host schema/Str})

(def cfg-schema
  {:lcmap.see (merge db-cfg-schema msg-cfg-schema)
   schema/Keyword schema/Any})

(def defaults
  {:ini *lcmap-config-ini*
   :args *command-line-args*
   :spec opt-spec
   :schema cfg-schema})
