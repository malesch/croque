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


(defn create-queue [path]
  ;; return a binary SingleChronicleQueue
  (.build (ChronicleQueueBuilder/single (str path "/source"))))


(defn create-appender
  "Returns a new appender instance"
  [queue]
  (component/start (appender/new-appender queue)))

(defn create-tailer
  "Returns a new tailer instance"
  [queue]
  (component/start (tailer/new-tailer queue)))

(defn state
  "Returns some information on the queue"
  [{:keys [queue]}]
  {:cycle (.cycle queue)
   :first-cycle (.firstCycle queue)
   :last-cycle (.lastCycle queue)
   :epoch (.epoch queue)
   :first-index (.firstIndex queue)
   :index-count (.indexCount queue)
   :source-id (.sourceId queue)
   :file-path (.. queue file getPath)})



;;
;; CroqueQueue component
;;

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
    (assoc component :queue nil)))


(defn new-croque-queue [config]
  (map->CroqueQueue config))