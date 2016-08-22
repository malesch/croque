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


;; Queue operations

(defn queue-state
  "Returns some information on the queue"
  [{:keys [queue]}]
  (let [q (:queue queue)]
    {:cycle       (.cycle q)
     :first-cycle (.firstCycle q)
     :last-cycle  (.lastCycle q)
     :epoch       (.epoch q)
     :first-index (.firstIndex q)
     :index-count (.indexCount q)
     :source-id   (.sourceId q)
     :file-path   (.. q file getPath)}))


;; Appender operations

(defn append-entry!
  [{:keys [appender]} data]
  (appender/append! appender data))

(defn appender-state
  [{:keys [appender]}]
  (appender/state appender))


;; Tailer operations

(defn next-entry
  [{:keys [tailer]}]
  (tailer/next tailer))

(defn rewind
  [{:keys [tailer]} n]
  (tailer/rewind tailer n))

(defn seek-index-position
  [{:keys [tailer]} ipos]
  (tailer/seek-index-position tailer ipos))

(defn seek-sequence-position
  [{:keys [tailer]} spos]
  (tailer/seek-sequence-position tailer spos))

(defn tailer-state
  [{:keys [tailer]}]
  (tailer/state tailer))

;;
;; CroqueQueue component
;;

(defrecord CroqueQueue [path]

  component/Lifecycle

  (start [component]
    (log/info "Starting CroqueQueue")
    (configure-logging!)
    (let [queue (create-queue path)]
      (assoc component :queue queue)))

  (stop [component]
    (log/info "Stopping CroqueQueue")
    (when-let [queue (:queue component)]
      (.close queue))
    (assoc component :queue nil)))

(defn new-croque-queue [config]
  (component/system-map
    :queue (map->CroqueQueue config)
    :appender (component/using (appender/new-appender) [:queue])
    :tailer (component/using (tailer/new-tailer) [:queue])))