(ns croque.listener
  (:require [taoensso.timbre :as log])
  (:import (java.io File)
           (java.text SimpleDateFormat)
           (net.openhft.chronicle.queue RollCycle)
           (net.openhft.chronicle.queue.impl StoreFileListener)))

(defn parse-filename-time
  "Parse the time from the file name using the provided pattern (missing time/date
  values are replaced with default values). If parsing fails, return nil."
  [pattern fname]
  (try
    (.getTime (.parse (SimpleDateFormat. pattern) fname))
    (catch Exception _ nil)))

(defn map-files-by-time
  "Returns a map from the list of Java File objects where the key is the parsed
  epoch time from the file name (pattern `date-format`). Files which can not be
  correctly parsed are silently ignored. The returned map is\n  ordered in
  ascending order."
  [date-format files]
  (into (sorted-map)
        (reduce (fn [acc f]
                  (if-let [t (parse-filename-time date-format (.getName f))]
                    (conj acc [t f])
                    acc)) [] files)))

(defn cycle-cleanup-listener
  "Return an implemation of a `StoreFileListener` listener which deletes released
   cycles from the file system. The param `retain` specifies the maximum number of
   cycle files to retain and to delete all other files."
  [path ^RollCycle rollCycle retain]
  (reify StoreFileListener
    (onReleased [_ cycle file]
      (log/debugf "`onReleased` called on cycle-cleanup-listener: cycle=%s, file=%s" cycle file)
      (let [cycle-files (->> (File. path)
                             (.listFiles)
                             (map-files-by-time (.format rollCycle)))
            delete-count (max 0 (- (count cycle-files) retain))]
        (when (pos? delete-count)
          (doseq [[_ f] (take delete-count cycle-files)]
            (log/infof "Delete cycle file: %s" f)
            (.delete f)))))))