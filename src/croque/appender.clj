(ns croque.appender
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.nippy :as nippy]
            [croque.util :refer [index->sequence]])
  (:import [net.openhft.chronicle.bytes Bytes]
           [net.openhft.chronicle.queue ExcerptAppender]))


(defn make-appender [{:keys [queue]}]
  (.acquireAppender queue))


(defn append!
  "Append data to the queue"
  [{:keys [appender]} data]
  (let [bytes (nippy/freeze data)]
    (.writeBytes appender (Bytes/allocateDirect bytes))))

(defn state
  "Return a map with state information on the appender instance"
  [{:keys [appender]}]
  {:source-id              (.sourceId appender)
   :cycle                  (.cycle appender)
   :last-index-appended    (.lastIndexAppended appender)
   :timeout                (.timeoutMS appender)
   :lazy-indexing          (.lazyIndexing appender)
   :file-path              (.. appender queue file getPath)
   :last-sequence-appended (index->sequence (.queue appender)
                                            (.lastIndexAppended appender))})


;;
;; CroqueQueue appender component
;;

(defrecord CroqueAppender [queue]
  component/Lifecycle

  (start [component]
    (log/info "Create CroqueAppender")
    (assoc component :appender (make-appender queue)))

  (stop [component]
    (log/info "Destroy CroqueAppender")
    (assoc component :appender nil)))


(defn new-appender
  ([]
   (new-appender {}))
  ([queue]
   (map->CroqueAppender queue)))
