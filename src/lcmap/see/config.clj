(ns lcmap.see.config
  (:require [lcmap.config.helpers :refer :all]
            [schema.core :as schema]))

(def opt-spec [])

(def see-schema
  {:lcmap.see {:db-hosts [schema/Str]
               :db-user schema/Str
               :db-pass schema/Str
               :job-keyspace schema/Str
               :job-table schema/Str}})

(def logging-schema
  {:lcmap.logging {:level schema/Str
                   :namespaces [schema/Str]}})
(def cfg-schema
  (merge see-schema
         logging-schema
         {schema/Keyword schema/Any}))

(def defaults
  {:ini *lcmap-config-ini*
   :args *command-line-args*
   :spec opt-spec
   :schema cfg-schema})
