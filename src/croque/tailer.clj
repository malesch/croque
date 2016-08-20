(ns croque.tailer
  (:refer-clojure :exclude [peek] :as core)
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [clojure.edn :as edn]
            [croque.util :refer [index->sequence sequence->index]])
  (:import [clojure.lang ExceptionInfo]
           [net.openhft.chronicle.queue ExcerptTailer]))


(defn make-tailer [{:keys [queue]}]
  (.createTailer queue))


(defn state
  "Return a map with state information on the tailer instance"
  [{:keys [^ExcerptTailer tailer]}]
  {:source-id  (.sourceId tailer)
   :cycle      (.cycle tailer)
   :direction  (keyword (str (.direction tailer)))
   :index      (.index tailer)
   :sequence   (index->sequence (.queue tailer)
                                (.index tailer))
   :file-path (.. tailer queue file getPath)})

(defn peek
  "Return the queue value from the current read position (sequence) and move to next position"
  [{:keys [tailer]}]
  (when-let [s (.readText tailer)]
    (edn/read-string s)))

(defn seek-index!
  "Set read position by the index position"
  [{:keys [tailer] :as component} index]
  (when-not (true? (.moveToIndex tailer index))
    (throw (ex-info "Invalid index position" {:index index
                                              :state (state component)}))))

(defn seek-sequence!
  "Set read position by the sequence number"
  [{:keys [tailer] :as component} sequence]
  (let [queue (.queue tailer)
        index (sequence->index queue sequence)]
    (try
      (seek-index! component index)
      (catch ExceptionInfo ex
        (throw (vary-meta ex assoc :sequence sequence))))))

(defn rewind!
  "Rewind the current read position by n steps"
  [component n]
  (when-let [{:keys [index]} (state component)]
    (seek-sequence! component (- index n))))


;;
;; CroqueQueue tailer component
;;

(defrecord CroqueTailer [queue]
  component/Lifecycle

  (start [component]
    (log/info "Create CroqueTailer")
    (assoc component :tailer (make-tailer queue)))

  (stop [component]
    (log/info "Destroy CroqueTailer")
    (assoc component :tailer nil)))


(defn new-tailer [queue]
  (->CroqueTailer queue))
