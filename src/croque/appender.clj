(ns croque.appender
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [croque.util :refer [index->sequence]])
  (:import [net.openhft.chronicle.queue ExcerptAppender]))

(defprotocol Appender
  "Protocol for a Chronical Queue appender instance"

  (append! [this x]
    "Append data to the queue")
  (state [this]
    "Return a map with state information on the appender instance"))


(defn make-appender [{:keys [queue]}]
  (.acquireAppender queue))

(defn write-text [{:keys [appender]} x]
  (.writeText appender (pr-str x)))

(defn appender-state
  "Return a map with state parameters on an appender instance."
  [{:keys [^ExcerptAppender appender]}]
  {:source-id              (.sourceId appender)
   :cycle                  (.cycle appender)
   :last-index-appended    (.lastIndexAppended appender)
   :timeout                (.timeoutMS appender)
   :lazy-indexing          (.lazyIndexing appender)
   :queue-path             (.. appender queue file getPath)
   :last-sequence-appended (index->sequence (.queue appender)
                                            (.lastIndexAppended appender))})

(defrecord CroqueAppender [queue]
  component/Lifecycle

  (start [component]
    (log/info "Create CroqueAppender")
    (assoc component :appender (make-appender queue)))

  (stop [component]
    (log/info "Destroy CroqueAppender")
    (assoc component :appender nil))

  Appender

  (append! [component x]
    (write-text component x))

  (state [component]
    (appender-state component)))


(defn new-appender [queue]
  (->CroqueAppender queue))
