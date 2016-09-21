(ns lcmap.see.config
  ""
  (:require [lcmap.config.helpers :as helpers]
            [lcmap.logger.config :as logger-cfg]
            [schema.core :as schema]))

(def opt-spec [])

(def see-schema
  {:lcmap.see {:db-hosts [schema/Str]
               :db-user schema/Str
               :db-pass schema/Str
               :job-keyspace schema/Str
               :job-table schema/Str
               :backend schema/Str
               schema/Keyword schema/Any}})

(def cfg-schema
  (merge see-schema
         logger-cfg/logger-schema
         {schema/Keyword schema/Any}))

(def defaults
  {:ini helpers/*lcmap-config-ini*
   :args *command-line-args*
   :spec opt-spec
   :schema cfg-schema})
