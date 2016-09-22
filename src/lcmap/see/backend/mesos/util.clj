(ns lcmap.see.backend.mesos.util
  (:import java.util.UUID))

(defn get-uuid
  "A Mesos/protobufs-friendly UUID wrapper."
  []
  (->> (UUID/randomUUID)
       (str)
       (assoc {} :value)))

(defn get-master
  [component]
  (str (get-in component [:see :mesos-host])
       ":"
       (get-in component [:see :mesos-port])))
