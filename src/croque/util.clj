(ns croque.util
  (:import (net.openhft.chronicle.queue.impl RollingChronicleQueue)
           (clojure.lang ExceptionInfo)))

(defn sequence-from-index
  "Return the sequence number from the index of the given queue.

  For the DAILY cycle the index is calculated as follows:
  index = ((long) cycle << 32) | sequence-number)."
  [^RollingChronicleQueue queue ^long index]
  (let [rc (.rollCycle queue)]
    (.toSequenceNumber rc index)))

(defn cycle-from-index
  "Return the cycle number from the index of the given queue."
  [^RollingChronicleQueue queue ^long index]
  (let [rc (.rollCycle queue)]
    (.toCycle rc index)))

(defn sequence->index
  "Calculate the index position from the sequence number for the
  given queue."
  [^RollingChronicleQueue queue ^long sequence]
  (let [rc (.rollCycle queue)
        q-cycle (.cycle queue)]
    (.toIndex rc q-cycle sequence)))

(defn merge-ex-data
  "Return a modified version of an ExceptionInfo exception where the exception
  data is merged with the data map parameter."
  [^ExceptionInfo ex dm]
  {:pre [(map? dm)]}
  (ex-info (.getMessage ex) (merge (ex-data ex) dm)))