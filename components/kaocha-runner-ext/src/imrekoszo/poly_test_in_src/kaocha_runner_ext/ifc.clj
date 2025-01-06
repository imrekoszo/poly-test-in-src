(ns imrekoszo.poly-test-in-src.kaocha-runner-ext.ifc
  (:require
    [polylith-kaocha.kaocha-test-runner.core :as kaocha-test-runner]))

(defn runner-opts->kaocha-poly-opts
  "Works together with the :ns-patterns in
  components/kaocha-wrapper-ext/resources/kaocha-wrapper-ext/tests.edn
  to make kaocha look for clojure.test tests in source files"
  [runner-opts]
  (-> runner-opts
    (kaocha-test-runner/runner-opts->kaocha-poly-opts)
    (as-> $ (update $ :test-paths into (:src-paths $)))))
