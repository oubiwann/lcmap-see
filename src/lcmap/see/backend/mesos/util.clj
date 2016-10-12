(ns lcmap.see.backend.mesos.util
  (:import java.util.UUID))

(defn get-uuid
  "A Mesos/protobufs-friendly UUID wrapper."
  []
  (->> (UUID/randomUUID)
       (str)
       (assoc {} :value)))

(defn get-host
  [backend-impl]
  (get-in backend-impl [:cfg :mesos-host]))

(defn get-master
  [backend-impl]
  (format "%s:%s"
          (get-in backend-impl [:cfg :mesos-host])
          (get-in backend-impl [:cfg :mesos-port])))
