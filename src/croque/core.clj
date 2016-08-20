(ns croque.core
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [croque.appender :as appender]
            [croque.tailer :as tailer])
  (:import [net.openhft.chronicle.queue ChronicleQueueBuilder]))


(defn configure-logging! []
  (log/merge-config! {:level :info
                      :appenders {:rotor (rotor/rotor-appender {:path     "croque.log"
                                                                :max-size (* 512 1024)
                                                                :backlog  10})}}))

(defprotocol Queue
  "Protocol for a Chronical Queue instance"

  (create-appender [this]
    "Returns a new appender instance")

  (create-tailer [this]
    "Returns a new tailer instance")

  (state [this]
    "Returns some information on the queue"))


(defn create-queue [path]
  ;; return a binary SingleChronicleQueue
  (.build (ChronicleQueueBuilder/single (str path "/source"))))

(defn queue-state [{:keys [queue]}]
  {:file-path (.getPath queue) })


(defrecord CroqueQueue [path]
  component/Lifecycle

  (start [component]
    (log/info "Starting CroqueQueue")
    (let [queue (create-queue path)]
      (assoc component :queue queue)))

  (stop [component]
    (log/info "Stopping CroqueQueue")
    (when-let [queue (:queue component)]
      (.close queue))
    (assoc component :queue nil))

  Queue

  (create-appender [component]
    (component/start (appender/new-appender component)))

  (create-tailer [component]
    (component/start (tailer/new-tailer component)))

  (state [component]
    (queue-state component)))

(defn new-croque-queue [config]
  (map->CroqueQueue config))