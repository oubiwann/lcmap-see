(defproject gov.usgs.eros/lcmap-see "1.0.0-SNAPSHOT"
  :parent-project {
    :coords [gov.usgs.eros/lcmap-system "1.0.0-SNAPSHOT"]
    :inherit [
      :deploy-repositories
      :license
      :managed-dependencies
      :plugins
      :pom-addition
      :repositories
      :target-path
      ;; XXX The following can be un-commented once this issue is resolved:
      ;;     * https://github.com/achin/lein-parent/issues/3
      ;; [:profiles [:uberjar :dev]]
      ]}
  :description "LCMAP Science Execution Environment"
  :url "https://github.com/USGS-EROS/lcmap-see"
  :dependencies [[org.clojure/clojure]
                 [org.clojure/core.match]
                 [org.clojure/data.codec]
                 [org.clojure/data.json]
                 [org.clojure/data.xml]
                 [org.clojure/core.memoize]
                 ;; Componentization
                 [com.stuartsierra/component]
                 ;; Logging and Error Handling -- note that we need to explicitly pull
                 ;; in a version of slf4j so that we don't get conflict messages on the
                 ;; console
                 [dire]
                 [slingshot]
                 ;; Job Tracker
                 [org.clojure/core.cache]
                 [co.paralleluniverse/pulsar]
                 [org.clojars.hozumi/clj-commons-exec]
                 [digest]
                 ;; DB
                 [clojurewerkz/cassaforte]
                 [net.jpountz.lz4/lz4]
                 [org.xerial.snappy/snappy-java]
                 ;; Distributed Computation
                 [spootnik/mesomatic]
                 [spootnik/mesomatic-async]
                 ;; LCMAP Components
                 [gov.usgs.eros/lcmap-config]
                 [gov.usgs.eros/lcmap-logger]
                 [gov.usgs.eros/lcmap-client-clj]
                 ;; XXX note that we may still need to explicitly include the
                 ;; Apache Java HTTP client, since the version used by the LCMAP
                 ;; client is more recent than that used by Chas Emerick's
                 ;; 'friend' library (the conflict causes a compile error which
                 ;; is worked around by explicitly including Apache Java HTTP
                 ;; client library).
                 ;; XXX temp dependencies:
                 [org.apache.httpcomponents/httpclient]
                 [clojure-ini]
                 [clj-http]
                 ;; Data types, encoding, etc.
                 [byte-streams]
                 [clj-time]
                 [commons-codec]
                 ;; Geospatial libraries
                 [clj-gdal]
                 ;; Dev and project metadata
                 [leiningen-core]]
  :plugins [[lein-parent "0.3.0"]]
  :source-paths ["src"]
  :java-agents [[co.paralleluniverse/quasar-core "0.7.6"]]
  :jvm-opts ["-Dco.paralleluniverse.fibers.detectRunawayFibers=false"]
  :repl-options {:init-ns lcmap.see.dev}
  :main lcmap.see.app
  :codox {:project {:name "lcmap.see"
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
    :uberjar {:aot :all}
    ;; configuration for dev environment -- if you need to make local changes,
    ;; copy `:env { ... }` into `{:user ...}` in your ~/.lein/profiles.clj and
    ;; then override values there
    :dev {
      :source-paths ["dev-resources/src"]
      :aliases {"see"
          ^{:doc (str "Command line interface for LCMAP SEE commands.\n"
                      "For more info run `lein see --help`\n")}
          ^:pass-through-help
          ["run" "-m" "lcmap.see.app"]}}})
