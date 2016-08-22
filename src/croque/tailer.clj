(ns croque.tailer
  (:refer-clojure :exclude [next] :as core)
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.nippy :as nippy]
            [croque.util :refer [index->sequence sequence->index]])
  (:import [clojure.lang ExceptionInfo]
           [net.openhft.chronicle.bytes Bytes]
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


(defn next
  "Returns the next value from queue. Returns nil if no value available."
  [{:keys [tailer]}]
  (let [using (Bytes/allocateElasticDirect)]
    (.readBytes tailer using)
    (when-not (.isEmpty using)
      (nippy/thaw (.toByteArray using)))))

(defn seek-index-position
  "Set read position by the index position"
  [{:keys [tailer] :as component} index]
  (when-not (true? (.moveToIndex tailer index))
    (throw (ex-info "Invalid index position" {:index index
                                              :state (state component)}))))

(defn seek-sequence-position
  "Set read position by the sequence number"
  [{:keys [tailer] :as component} sequence]
  (let [queue (.queue tailer)
        index (sequence->index queue sequence)]
    (try
      (seek-index-position component index)
      (catch ExceptionInfo ex
        (throw (vary-meta ex assoc :sequence sequence))))))

(defn rewind
  "Rewind the current read position by n modifications"
  [component n]
  (when-let [{:keys [index]} (state component)]
    (seek-sequence-position component (- index n))))


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
