(ns croque.core-test
  (:require [clojure.test :refer :all]
            [croque.core :refer :all]
            [croque.helper :refer [with-components random-path]]))

(deftest creation-test
  (testing "Creation of queue, appender and tailer instances"
    (with-components [croque (new-croque-queue {:path (random-path)})]
      (is (map? (queue-state croque)) "No queue state available")
      (is (map? (appender-state croque)) "No appender state available")
      (is (map? (tailer-state croque)) "No tailer state available"))))

(deftest simple-append-read-test
  (testing "Create queue, append and read back test messages"
    (with-components [croque (new-croque-queue {:path (random-path)})]
      ;; add three messages
      (append-entry! croque {:foo 1})
      (append-entry! croque {:foo 2})
      (append-entry! croque {:foo 3})
      (is (= 2 (:last-sequence-appended (appender-state croque))) "Wrong appender index")
      (is (= {:foo 1} (next-entry croque)) "Wrong first entry")
      (is (= {:foo 2} (next-entry croque)) "Wrong second entry")
      (is (= {:foo 3} (next-entry croque)) "Wrong third entry")
      (is (nil? (next-entry croque)) "No next-entry entry expected"))))

(deftest rewind-test
  (testing "Rewind function"
    (with-components [croque (new-croque-queue {:path (random-path)})]
      (append-entry! croque :1st-entry)
      (append-entry! croque :2nd-entry)
      (next-entry croque)                                  ;; :1st-entry
      (next-entry croque)                                  ;; :2nd-entry
      (rewind croque 2)
      (is (= :1st-entry (next-entry croque)) "Wrong first entry after rewind"))))

(deftest seek-sequence-position-test
  (testing "Seek sequence position function"
    (with-components [croque (new-croque-queue {:path (random-path)})]
      (append-entry! croque [:entry {:num 1}])
      (append-entry! croque [:entry {:num 2}])
      (next-entry croque)                                  ;; [:entry {:num 1}]
      (next-entry croque)                                  ;; [:entry {:num 2}]
      (seek-sequence-position croque 0)
      (is (= [:entry {:num 1}] (next-entry croque)) "Wrong first entry after seek sequence position"))))

(deftest seek-index-position-test
  (testing "Seek index position function"
    (with-components [croque (new-croque-queue {:path (random-path)})]
      (append-entry! croque #{:first-entry})
      (let [idx (:last-index-appended (appender-state croque))]
        (append-entry! croque #{:second-entry})
        (next-entry croque)                                ;; #{:first-entry}
        (next-entry croque)                                ;; #{:second-entry}
        (seek-index-position croque idx)
        (is (= #{:first-entry} (next-entry croque)) "Wrong first entry after seek index position")))))

(deftest with-tailer-restore-test
  (testing "Test for the `with-tailer-restore` macro"
    (with-components [croque (new-croque-queue {:path (random-path)})]
      ;; prepare values
      (append-entry! croque {:entry 0})
      (append-entry! croque {:entry 1})
      (append-entry! croque {:entry 2})
      ;; read values
      (next-entry croque)                                   ;; {:entry 0}
      (next-entry croque)                                   ;; {:entry 1}
      (with-tailer-restore croque
                           (rewind croque 2)
                           (is (= (next-entry croque) {:entry 0}) "Wrong index position within `with-index-position` macro"))
      (is (= (next-entry croque) {:entry 2}) "Wrong index position after `with-index-position` macro"))))
