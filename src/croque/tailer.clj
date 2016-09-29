(ns croque.tailer
  (:refer-clojure :exclude [next] :as core)
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.nippy :as nippy]
            [croque.util :as util])
  (:import (clojure.lang ExceptionInfo)
           (net.openhft.chronicle.queue ExcerptTailer)))


(defn make-tailer [{:keys [queue]}]
  (.createTailer queue))


(defn state
  "Return a map with state information on the tailer instance"
  [{:keys [^ExcerptTailer tailer]}]
  {:source-id  (.sourceId tailer)
   :cycle      (.cycle tailer)
   :direction  (keyword (str (.direction tailer)))
   :index      (.index tailer)
   :sequence   (util/sequence-from-index (.queue tailer)
                                         (.index tailer))
   :file-path (.. tailer queue file getPath)})


(defn next-entry
  "Returns the next value from queue. Returns nil if no value available."
  [{:keys [tailer]}]
  (with-open [context (.readingDocument tailer)]
    (when (.isPresent context)
      (let [data-bytes (.. context (wire) (read) (bytes))]
        (nippy/thaw data-bytes)))))

(defn seek-index-position
  "Set read position by the index position"
  [{:keys [tailer] :as component} ipos]
  (when-not (true? (.moveToIndex tailer ipos))
    (throw (ex-info "Invalid index position" {:index ipos
                                              :state (state component)}))))

(defn seek-sequence-position
  "Set read position by the sequence number"
  [{:keys [tailer] :as component} spos]
  (let [queue (.queue tailer)
        index (util/sequence->index queue spos)]
    (try
      (seek-index-position component index)
      (catch ExceptionInfo ex
        (throw (util/merge-ex-data ex {:sequence sequence}))))))

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


(defn new-tailer
  ([]
   (new-tailer {}))
  ([queue]
   (map->CroqueTailer queue)))
