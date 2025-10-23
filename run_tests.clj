#! /usr/bin/env bb

(require '[clojure.test :as test])

(require '[what.actions-handling-test])
(require '[what.database-test])
(require '[what.utils-test])


(def test-results
  (test/run-tests 'what.utils-test 'what.database-test 'what.actions-handling-test))


(let [{:keys [fail error]} test-results]
  (when (pos? (+ fail error))
    (System/exit 1)))
