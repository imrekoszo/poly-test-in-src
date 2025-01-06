(ns imrekoszo.poly-test-in-src.src-only-workaround.ifc
  (:require
    [clojure.test :refer [is]]))

(defn bad
  {:test #(is (= :foo (bad)))}
  []
  :bar)
