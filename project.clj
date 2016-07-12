(defproject gov.usgs.eros/lcmap-see "0.5.0-SNAPSHOT"
  :description "LCMAP Science Execution Environment"
  :url "https://github.com/USGS-EROS/lcmap-see"
  :license {:name "NASA Open Source Agreement, Version 1.3"
            :url "http://ti.arc.nasa.gov/opensource/nosa/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/core.memoize "0.5.9"]
                 ;; Componentization
                 [com.stuartsierra/component "0.3.1"]
                 ;; Logging and Error Handling -- note that we need to explicitly pull
                 ;; in a version of slf4j so that we don't get conflict messages on the
                 ;; console
                 [dire "0.5.4"]
                 [slingshot "0.12.2"]
                 ;; Job Tracker
                 [org.clojure/core.memoize "0.5.9"] ; These two are not used directly, but
                 [org.clojure/core.cache "0.6.5"]   ; without them an exception is raised
                 [co.paralleluniverse/pulsar "0.7.5"]
                 [org.clojars.hozumi/clj-commons-exec "1.2.0"]
                 [digest "1.4.4"]
                 ;; DB
                 [clojurewerkz/cassaforte "2.0.2"]
                 [net.jpountz.lz4/lz4 "1.3.0"]
                 [org.xerial.snappy/snappy-java "1.1.2.6"]
                 ;; Distributed Computation
                 [clojusc/mesomatic "0.28.2-SNAPSHOT"]
                 ;; LCMAP Components
                 [gov.usgs.eros/lcmap-config "0.5.0-SNAPSHOT"]
                 [gov.usgs.eros/lcmap-logger "0.5.0-SNAPSHOT"]
                 [gov.usgs.eros/lcmap-client-clj "0.5.0-SNAPSHOT"]
                 ;; XXX note that we may still need to explicitly include the
                 ;; Apache Java HTTP client, since the version used by the LCMAP
                 ;; client is more recent than that used by Chas Emerick's
                 ;; 'friend' library (the conflict causes a compile error which
                 ;; is worked around by explicitly including Apache Java HTTP
                 ;; client library).
                 ;; XXX temp dependencies:
                 [org.apache.httpcomponents/httpclient "4.5.2"]
                 [clojure-ini "0.0.2"]
                 [clj-http "3.1.0"]
                 ;; Data types, encoding, etc.
                 [byte-streams "0.2.2"]
                 [clj-time "0.12.0"]
                 [commons-codec "1.10"]
                 ;; Geospatial libraries
                 [clj-gdal "0.3.5-SNAPSHOT"]
                 ;; Dev and project metadata
                 [leiningen-core "2.6.1"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-pprint "1.1.2"]
            [lein-codox "0.9.5"]
            [lein-simpleton "1.3.0"]]
  :source-paths ["src" "test/support/auth-server/src"]
  :java-agents [[co.paralleluniverse/quasar-core "0.7.3"]]
  :jvm-opts ["-Dco.paralleluniverse.fibers.detectRunawayFibers=false"]
  :repl-options {:init-ns lcmap.see.dev}
  :main lcmap.see.app
  :target-path "target/%s"
  :codox {:project {:name "LCMAP SEE Library and Services"
                    :description "The Science and Execution Library & Services for the USGS Land Change Monitoring Assessment and Projection (LCMAP) Computation and Analysis Platform"}
          :namespaces [#"^lcmap.see\."]
          :output-path "docs/master/current"
          :doc-paths ["docs/source"]
          :metadata {:doc/format :markdown
                     :doc "Documentation forthcoming"}}
  ;; List the namespaces whose log levels we want to control; note that if we
  ;; add more dependencies that are chatty in the logs, we'll want to add them
  ;; here.
  :logging-namespaces [lcmap.see
                       lcmap.client
                       com.datastax.driver
                       co.paralleluniverse]
  :profiles {
    ;; configuration for dev environment -- if you need to make local changes,
    ;; copy `:env { ... }` into `{:user ...}` in your ~/.lein/profiles.clj and
    ;; then override values there
    :dev {
      :dependencies [[org.clojure/tools.namespace "0.3.0-alpha3"]
                     [slamhound "1.5.5"]]
      :aliases {"slamhound" ["run" "-m" "slam.hound"]}
      :source-paths ["dev-resources/src"]
      :plugins [[lein-kibit "0.1.2"]
                [jonase/eastwood "0.2.3"]]
      :env
        {:active-profile "development"
         :db {:hosts ["127.0.0.1"]
              :port 9042
              :protocol-version 3
              :keyspace "lcmap"
              :credentials {
                :username nil
                :password nil}}
          :log-level :debug}}
    ;; configuration for testing environment
    :testing {
      :env
        {:active-profile "testing"
         :db {}
         :http {}
         :log-level :debug}}
    ;; configuration for staging environment
    :staging {
      :env
        {:active-profile "staging"
         :db {}
         :http {}
         :log-level :warn}}
    ;; configuration for production environment
    :prod {
      :env
        {:active-profile "production"
         :db {}
         :http {}
         :log-level :error}}})
