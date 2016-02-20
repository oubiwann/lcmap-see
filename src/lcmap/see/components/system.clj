(ns ^{:doc
  "Top-level LCMAP SEE system component

  For more information, see the module-level code comments in
  ``lcmap.see.components``."}
  lcmap.see.components.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defrecord LCMAPSEESystem []
  component/Lifecycle

  (start [component]
    (log/info "LCMAP SEE system dependencies started; finishing LCMAP SEE startup ...")
    ;; XXX add any start-up needed for system as a whole
    (log/debug "LCMAP SEE System startup complete.")
    component)

  (stop [component]
    (log/info "Shutting down top-level LCMAP SEE ...")
    ;; XXX add any tear-down needed for system as a whole
    (log/debug "Top-level shutdown complete; shutting down system dependencies ...")
    component))

(defn new-lcmap-see-toplevel []
  (->LCMAPSEESystem))
