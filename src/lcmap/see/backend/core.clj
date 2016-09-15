(ns lcmap.see.backend.core)

(defprotocol ExecutionBackend
  "An interface that all backends need to implement."
  (set-up [this] "Connecting clients, instantiating drivers, or starting services.")
  (tear-down [this] "Reversing everything done in set-up."))
