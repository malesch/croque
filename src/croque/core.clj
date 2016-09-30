(ns croque.core
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [croque.appender :as appender]
            [croque.tailer :as tailer]
            [croque.listener :refer [cycle-cleanup-listener]])
  (:import (net.openhft.chronicle.queue ChronicleQueueBuilder RollCycles)))


(defn configure-logging! []
  (log/merge-config! {:level :info
                      :appenders {:rotor (rotor/rotor-appender {:path     "croque.log"
                                                                :max-size (* 512 1024)
                                                                :backlog  10})}}))


(defn resolve-roll-cycle [roll-cycle]
  (when roll-cycle
    (try
      (RollCycles/valueOf (name roll-cycle))
      (catch Exception _
        (log/errorf "Invalid roll-cycle configuration: %s" roll-cycle)
        (log/info "Fallback to default `DAILY`")
        (RollCycles/DAILY)))))

(defn create-queue [path roll retain]
  (let [rollCycle (resolve-roll-cycle roll)]
    ;; return a binary SingleChronicleQueue
    (cond-> (ChronicleQueueBuilder/single path)
            roll (.rollCycle rollCycle)
            retain (.storeFileListener (cycle-cleanup-listener path rollCycle retain))
            true (.build))))

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
  (tailer/next-entry tailer))

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

(defrecord CroqueQueue [path roll-cycle retain-cycles]

  component/Lifecycle

  (start [component]
    (log/infof "Starting CroqueQueue [path=%s, roll-cycle=%s, retain-cycles=%s]"
               path roll-cycle retain-cycles)
    (configure-logging!)
    (let [queue (create-queue path roll-cycle retain-cycles)]
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