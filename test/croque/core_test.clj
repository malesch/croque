(ns croque.core-test
  (:require [clojure.test :refer :all]
            [croque.core :refer :all]
            [croque.appender :as appender]
            [croque.test-utils :refer [with-components]]
            [croque.appender :as appender]
            [croque.tailer :as tailer]))

(deftest creation-test
  (testing "Creation of queue, appender and tailer instances"
    (with-components [queue (new-croque-queue {:path "target/creation"})]
      (let [appender (create-appender queue)
            tailer (create-tailer queue)]
        (is (map? (state queue)) "No queue state available")
        (is (map? (appender/state appender)) "No appender state available")
        (is (map? (tailer/state tailer)) "No tailer state available")))))

(deftest simple-append-read-test
  (testing "Create queue, append and read back test messages"
    (with-components [queue (new-croque-queue {:path "target/append-next"})]
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
