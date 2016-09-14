(ns croque.util
  (:import (net.openhft.chronicle.queue.impl RollingChronicleQueue)))

(defn index->sequence
  "Calculate the sequence position from the index of the given queue.

  For the DAILY cycle the index is calculated as follows:
  index = ((long) cycle << 32) | sequence-number)."
  [^RollingChronicleQueue queue ^long index]
  (let [rc (.rollCycle queue)]
    (.toSequenceNumber rc index)))

(defn sequence->index
  "Calculate the index position from the sequence number for the
  given queue."
  [^RollingChronicleQueue queue ^long sequence]
  (let [rc (.rollCycle queue)
        q-cycle (.cycle queue)]
    (.toIndex rc q-cycle sequence)))
