(ns what.utils-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [what.utils :as utils]))


(deftest prompted-input-test
  (let [prompt "Type something: "
        input-string "asdfg"
        result (with-in-str input-string (utils/prompted-input prompt))
        output-value (with-out-str (with-in-str input-string (utils/prompted-input prompt)))]
    (is (= [output-value result] [prompt input-string])))
  (let [prompt ""
        input-string "asdfg"
        result (with-in-str input-string (utils/prompted-input prompt))
        output-value (with-out-str (with-in-str input-string (utils/prompted-input prompt)))]
    (is (= [output-value result] [prompt input-string])))
  (let [prompt "Type something: "
        input-string ""
        result (with-in-str input-string (utils/prompted-input prompt))
        output-value (with-out-str (with-in-str input-string (utils/prompted-input prompt)))]
    (is (= [output-value result] [prompt nil])))
  (let [prompt ""
        input-string ""
        result (with-in-str input-string (utils/prompted-input prompt))
        output-value (with-out-str (with-in-str input-string (utils/prompted-input prompt)))]
    (is (= [output-value result] [prompt nil]))))


(deftest process-string-list-test
  (testing "Integers common case"
    (is (= [0 1 2]
           (utils/process-string-list "0 1 2" Integer/parseInt))))
  (testing "Integers positive, zero and negative"
    (is (= [-2 0 3]
           (utils/process-string-list "-2 0 3" Integer/parseInt))))
  (testing "Strings common case"
    (is (= ["ALPHA" "BETA" "GAMMA"]
           (utils/process-string-list "alpha beta gamma" string/upper-case))))
  (testing "Integers with additional spaces"
    (is (= [0 1 2]
           (utils/process-string-list "0   1  2" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "  0   1  2" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "0   1  2   " Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "  0   1  2   " Integer/parseInt)))
    )
  (testing "String with additional spaces"
    (is (= ["ALPHA" "BETA" "GAMMA"]
           (utils/process-string-list "alpha   beta  gamma" string/upper-case)))
    (is (= ["ALPHA" "BETA" "GAMMA"]
           (utils/process-string-list "alpha   beta  gamma" string/upper-case)))
    (is (= ["ALPHA" "BETA" "GAMMA"]
           (utils/process-string-list "  alpha   beta  gamma" string/upper-case)))
    (is (= ["ALPHA" "BETA" "GAMMA"]
           (utils/process-string-list "  alpha   beta  gamma   " string/upper-case))))
  (testing "Just one value"
    (is (= [42]
           (utils/process-string-list "42" Integer/parseInt))))
  (testing "Just one value with additional spaces"
    (is (= [42]
           (utils/process-string-list " 42    " Integer/parseInt))))
  (testing "No value"
    (is (= []
           (utils/process-string-list "" Integer/parseInt))))
  (testing "Just spaces"
    (is (= []
           (utils/process-string-list " " Integer/parseInt)))
    (is (= []
           (utils/process-string-list "   " Integer/parseInt))))
  (testing "With tabs"
    (is (= [0 1 2]
           (utils/process-string-list "0\t1\t2" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "0\t1\t2\t" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "\t0\t1\t2" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "\t0\t1\t2\t" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "\t\t0\t\t\t1\t2\t\t" Integer/parseInt))))
  (testing "With tabs and spaces"
    (is (= [0 1 2]
           (utils/process-string-list "  0  \t 1  2 \t" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "\t  0  \t 1  2" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "\t0  \t 1  2\t" Integer/parseInt)))
    (is (= [0 1 2]
           (utils/process-string-list "\t\t0  \t  \t 1  2\t\t\t" Integer/parseInt))))
  (testing "Just tabs"
    (is (= []
           (utils/process-string-list "\t" Integer/parseInt)))
    (is (= []
           (utils/process-string-list "\t\t\t" Integer/parseInt)))))
