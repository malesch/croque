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

(deftest simple-append-peek-test
  (testing "Create queue, append entries and peek test messages"
    (with-components [queue (new-croque-queue {:path "target/append-peek"})]
      (let [appender (create-appender queue)
            tailer (create-tailer queue)]
        ;; add three messages
        (appender/append! appender {:foo 1})
        (appender/append! appender {:foo 2})
        (appender/append! appender {:foo 3})
        (is (= 2 (:last-sequence-appended (appender/state appender))) "Wrong appender index")
        (is (= {:foo 1} (tailer/peek tailer)) "Wrong first message")
        (is (= {:foo 2} (tailer/peek tailer)) "Wrong second message")
        (is (= {:foo 3} (tailer/peek tailer)) "Wrong third message")
        (is (nil? (tailer/peek tailer)) "No message expected")))))
