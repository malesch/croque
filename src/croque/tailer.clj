(ns croque.tailer
  (:refer-clojure :exclude [peek] :as core)
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [clojure.edn :as edn]
            [croque.util :refer [index->sequence sequence->index]])
  (:import [clojure.lang ExceptionInfo]
           [net.openhft.chronicle.queue ExcerptTailer]))

(defprotocol Tailer
  "Protocol for a Chronical Queue tailer instance"

  (peek [this]
    "Return the queue value from the current read position (sequence) and move to next position")
  (rewind! [this n]
    "Rewind the current read position by n steps")
  (seek-index! [this idx]
    "Set read position by the index position")
  (seek-sequence! [this sequence]
    "Set read position by the sequence number")
  (state [this]
    "Return a map with state information on the tailer instance"))


(defn make-tailer [{:keys [queue]}]
  (.createTailer queue))

(defn read-text [{:keys [tailer]}]
  (when-let [x (.readText tailer)]
    (edn/read-string x)))

(defn tailer-state
  "Return a map with state parameter on a tailer instance."
  [{:keys [^ExcerptTailer tailer]}]
  {:source-id  (.sourceId tailer)
   :cycle      (.cycle tailer)
   :direction  (keyword (str (.direction tailer)))
   :index      (.index tailer)
   :sequence   (index->sequence (.queue tailer)
                                (.index tailer))
   :queue-path (.. tailer queue file getPath)})


(defrecord CroqueTailer [queue]
  component/Lifecycle

  (start [component]
    (log/info "Create CroqueTailer")
    (assoc component :tailer (make-tailer queue)))

  (stop [component]
    (log/info "Destroy CroqueTailer")
    (assoc component :tailer nil))

  Tailer

  (peek [component]
    (read-text component))

  (rewind! [component n]
    (when-let [{:keys [index]} (tailer-state component)]
      (seek-sequence! component (- index n))))

  (seek-index! [{:keys [tailer] :as component} index]
    (when-not (true? (.moveToIndex tailer index))
      (throw (ex-info "Invalid index position" {:index index
                                                :state (state component)}))))

  (seek-sequence! [{:keys [tailer] :as component} sequence]
    (let [queue (.queue tailer)
          index (sequence->index queue sequence)]
      (try
        (seek-index! component index)
        (catch ExceptionInfo ex
          (throw (vary-meta ex assoc :sequence sequence))))))

  (state [component]
    (tailer-state component)))

(defn new-tailer [queue]
  (->CroqueTailer queue))
