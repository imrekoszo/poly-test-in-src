(ns imrekoszo.poly-test-in-src.src-only.ifc
  (:require
    [clojure.test :refer [is]]))

(defn bad
  {:test #(is (= :foo (bad)))}
  []
  :bar)
