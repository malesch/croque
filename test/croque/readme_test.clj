(ns croque.readme-test
  (:require [clojure.test :refer :all]
            [croque.core :refer :all]
            [croque.helper :refer [with-components random-path]]))

(deftest readme-test
  (testing "Perform as test the usage example from the README"
    (with-components [queue (new-croque-queue {:path (random-path)})]
      (append-entry! queue [:example {:entry 0}])
      (append-entry! queue [:example {:entry 1}])
      (append-entry! queue [:example {:entry 2}])

      (is (= [:example {:entry 0}] (next-entry queue)) "Failed reading first entry")
      (is (= [:example {:entry 1}] (next-entry queue)) "Failed reading second entry")
      (is (= [:example {:entry 2}] (next-entry queue)) "Failed reading third entry")

      (let [last-idx-pos (:last-index-appended (appender-state queue))]
        (append-entry! queue [:example {:entry 3}])
        (rewind queue 2)
        (is (= [:example {:entry 1}] (next-entry queue)) "`rewind` failed")

        (seek-sequence-position queue 3)
        (is (= [:example {:entry 3}] (next-entry queue)) "`seek-sequence-position` failed")

        (seek-index-position queue last-idx-pos)
        (is (= [:example {:entry 2}] (next-entry queue)) "`seek-index-position` failed")))))
