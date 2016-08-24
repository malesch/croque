(ns croque.listener-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [croque.helper :refer [random-path]]
            [croque.listener :refer :all])
  (:import [java.io File]
           [java.util Date]
           [net.openhft.chronicle.queue RollCycles]
           (java.text SimpleDateFormat)))

(deftest parse-filename-time-test
  (testing "Test parsing out the time from the filename"
    (let [pattern "yyyy-MM-dd_HH:mm:ss"]
      (is (= nil (parse-filename-time pattern "foo.cq4")))
      (is (= nil (parse-filename-time pattern "1970X-01-01_01:00:00")))
      (is (= 0 (parse-filename-time pattern "1970-01-01_01:00:00")))
      (is (= 0 (parse-filename-time pattern "1970-01-01_01:00:00.cq4"))))))


(defn- guaranteed-shuffle
  "Return a shuffled version of the input value and guarantee that it is
  not equal."
  [coll]
  (first (filter (partial not= coll) (repeatedly #(shuffle coll)))))

(defn- generate-file-list
  "Returns a list of File objects based on the list of file names. The returned
  list is guaranteed to have a different order than the input list."
  [names]
  (map #(File. %) (guaranteed-shuffle names)))

(defn- select-filenames
  "Return the filenames for the list of File objects."
  [files]
  (map #(.getName %) files))

(deftest map-files-by-time-test
  (testing "Simple file list"
    (let [expected ["20162307-0945.cq4" "20162407-0945.cq4" "20162408-0944.cq4" "20162408-0945.cq4"]]
      (is (= expected
             (select-filenames (vals (map-files-by-time "yyyyMMdd-HHmm" (generate-file-list expected)))))))
    (let [expected (map #(str % ".cq4") (range 1995 2020))]
      (is (= expected
             (select-filenames (vals (map-files-by-time "yyyy" (generate-file-list expected))))))))

  (testing "List including invalid names"
    (let [expected ["20162307-0945.cq4" "20162407-0945.cq4" "20162408-0944.cq4" "20162408-0945.cq4"]
          invalids ["201607-0945.cq4" "201623x7-0945.cq4" "201623x7-0945.cq4" "20162307.cq4" "20162408-09x5.cq4"]]
      (is (= expected
             (select-filenames (vals (map-files-by-time "yyyyMMdd-HHmm" (generate-file-list (concat expected invalids))))))))))

(defn- rand-date
  "Return a random date between epoch and now."
  []
  (Date. (long (* (rand) (System/currentTimeMillis)))))

(defn- setup-test-files
  "Create under a directory path n test files following the given date-time pattern."
  [path pattern n]
  (let [fp (File. path)
        sdf (SimpleDateFormat. pattern)]
    (assert (not (.exists fp)) "The test directory for `setup-test-files` must not already exist!")
    (.mkdirs fp)
    (every? true?
            (map (fn [d] (->> d
                              (.format sdf)
                              (File. fp)
                              (.createNewFile)))
                 (take n (repeatedly rand-date))))))

(deftest cycle-cleanup-listener-test
  (log/set-level! :error)
  (let [run-test (fn [rollCycle cnt]
                   (let [path (random-path)
                         listener (cycle-cleanup-listener path rollCycle cnt)]
                     (setup-test-files path (str (.format rollCycle) "'.cq4'") (* 5 cnt))
                     (.onReleased listener 0 (File. ""))
                     (is (= cnt (count (.list (File. path))))
                         (format "Test [%s, %d] has wrong number of remaining files" rollCycle cnt))))]
    (run-test RollCycles/TEST_SECONDLY 5)
    (run-test RollCycles/MINUTELY 5)
    (run-test RollCycles/DAILY 5)))
