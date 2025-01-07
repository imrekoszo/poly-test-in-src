(ns imrekoszo.poly-test-in-src.dummy-test.ifc
  (:require
    [clojure.test :refer [is]]))

(defn bad
  {:test #(is (= :foo (bad)))}
  []
  :bar)
