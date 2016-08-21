(ns croque.core-test
  (:require [clojure.test :refer :all]
            [croque.core :refer :all]
            [croque.appender :as appender]
            [croque.test-utils :refer [with-components random-path]]
            [croque.appender :as appender]
            [croque.tailer :as tailer]))

(deftest creation-test
  (testing "Creation of queue, appender and tailer instances"
    (with-components [queue (new-croque-queue {:path (random-path)})]
      (let [appender (create-appender queue)
            tailer (create-tailer queue)]
        (is (map? (state queue)) "No queue state available")
        (is (map? (appender/state appender)) "No appender state available")
        (is (map? (tailer/state tailer)) "No tailer state available")))))

(deftest simple-append-read-test
  (testing "Create queue, append and read back test messages"
    (with-components [queue (new-croque-queue {:path (random-path)})]
      (let [appender (create-appender queue)
            tailer (create-tailer queue)]
        ;; add three messages
        (appender/append! appender {:foo 1})
        (appender/append! appender {:foo 2})
        (appender/append! appender {:foo 3})
        (is (= 2 (:last-sequence-appended (appender/state appender))) "Wrong appender index")
        (is (= {:foo 1} (tailer/next tailer)) "Wrong first entry")
        (is (= {:foo 2} (tailer/next tailer)) "Wrong second entry")
        (is (= {:foo 3} (tailer/next tailer)) "Wrong third entry")
        (is (nil? (tailer/next tailer)) "No next entry expected")))))

(deftest rewind-test
  (testing "Rewind function"
    (with-components [queue (new-croque-queue {:path (random-path)})]
      (let [appender (create-appender queue)
            tailer (create-tailer queue)]
        (appender/append! appender :1st-entry)
        (appender/append! appender :2nd-entry)
        (tailer/next tailer)                                ;; :1st-entry
        (tailer/next tailer)                                ;; :2nd-entry
        (tailer/rewind tailer 2)
        (is (= :1st-entry (tailer/next tailer)) "Wrong first entry after rewind")))))

(deftest seek-sequence-position-test
  (testing "Seek sequence position function"
    (with-components [queue (new-croque-queue {:path (random-path)})]
      (let [appender (create-appender queue)
            tailer (create-tailer queue)]
        (appender/append! appender [:entry {:num 1}])
        (appender/append! appender [:entry {:num 2}])
        (tailer/next tailer)                                ;; [:entry {:num 1}]
        (tailer/next tailer)                                ;; [:entry {:num 2}]
        (tailer/seek-sequence-position tailer 0)
        (is (= [:entry {:num 1}] (tailer/next tailer)) "Wrong first entry after seek sequence position")))))

(deftest seek-index-position-test
  (testing "Seek index position function"
    (with-components [queue (new-croque-queue {:path (random-path)})]
      (let [appender (create-appender queue)
            tailer (create-tailer queue)]
        (appender/append! appender #{:first-entry})
        (let [idx (:last-index-appended (appender/state appender))]
          (appender/append! appender #{:second-entry})
          (tailer/next tailer)                              ;; #{:first-entry}
          (tailer/next tailer)                              ;; #{:second-entry}
          (tailer/seek-index-position tailer idx)
          (is (= #{:first-entry} (tailer/next tailer)) "Wrong first entry after seek index position"))))))
