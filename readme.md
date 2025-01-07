# poly-test-in-src

Demonstrates a case where [polylith-external-test-runner](https://github.com/seancorfield/polylith-external-test-runner) does not see tests in a component (`src-only`) which only has tests in its source files (via `:test` metadata) and does not have separate test sources.

## Potential reasons

### I might not have configured the external test runner correctly

This is always a possibility, however the configuration of that runner is nice and simple. See `workspace.edn > :test-configs > :external`.

### Edge case not handled in code

[This line](https://github.com/polyfy/polylith/blob/2d850fb1e0b77ae335a7bd8f96c8f2b2ca18b469/components/test/src/polylith/clj/core/test/bricks_to_test.clj#L19) in the Polylith codebase only considers bricks for testing that have test sources. This is why I had to implement a hacky workaround in [polylith-kaocha](https://github.com/imrekoszo/polylith-kaocha). See the hack [part 1](https://github.com/imrekoszo/polylith-kaocha/blob/e0c6c4d192a69d52471232861e7129dcff0b496c/components/kaocha-test-runner/src/polylith_kaocha/kaocha_test_runner/bricks_to_test.clj#L2), [part 2](https://github.com/imrekoszo/polylith-kaocha/blob/e0c6c4d192a69d52471232861e7129dcff0b496c/components/kaocha-test-runner/src/polylith_kaocha/kaocha_test_runner/bricks_to_test.clj#L27-L30), and [how it's used](https://github.com/imrekoszo/polylith-kaocha/blob/a38bd8b3d760d34aa402f58b3732839fb7b34951/components/kaocha-test-runner/src/polylith_kaocha/kaocha_test_runner/core.clj#L24-L27).

I would love to not have to depend on a hack like this, and it would be very nice to be able to have this extended list of bricks available under a key on the `project` map, but I'm not sure whether that aligns with the Polylith team's goals. In [polylith#448](https://github.com/polyfy/polylith/issues/448) it is stated that 3rd party test runners can implement running tests from src directories, but calculating `bricks-to-test` is not trivial (due to changes, selections etc.) and it would be better to rely on Polylith there.

I present workarounds below that work varying across test runners but which isn't intuitive to find, doesn't look idiomatic, and potentially goes against Polylith norms.

## Repro

Note that this runs tests using all (default, external, kaocha) runners, asking to include src dir tests as well, but only kaocha finds the failing test in src. (The default test runner is not designed with this option in mind so that's expected to work like this.)

```text
; clojure -Srepro -M:poly test project:normal with:default:external:kaocha:include-src-dir
Projects to run tests from: normal

Test runner options:
  :include-src-dir => true

Running tests for the normal project using test runner: Polylith built-in clojure.test runner...
Running tests from the normal project, including 2 bricks: has-test-paths, kaocha-wrapper-ext

Testing imrekoszo.poly-test-in-src.has-test-paths.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.
Running tests for the normal project using test runner: Polylith org.corfield.external-test-runner...
Running tests from the normal project, including 2 bricks: has-test-paths, kaocha-wrapper-ext

Testing imrekoszo.poly-test-in-src.has-test-paths.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

No tests found in imrekoszo.poly-test-in-src.has-test-paths.ifc
Running tests for the normal project using test runner: polylith-kaocha...
[(.)(F)]
Randomized with --seed 1115509637

FAIL in imrekoszo.poly-test-in-src.src-only.ifc/bad (ifc.clj:6)
Expected:
  :foo
Actual:
  -:foo +:bar
2 tests, 2 assertions, 1 failures.
Tests failed [polylith-kaocha]
```

## Workaround 1: duplicate src paths as test paths

A workaround (that I found in [poly-rcf](https://github.com/ieugen/poly-rcf) by @ieugen) exists: if a brick does not have test paths, duplicate its source paths (that contain tests) in its test alias like so:

```clojure
{:paths ["src"]
 :deps {}
 :aliases {:test {:extra-paths ["src"]}}}
 ```

### This makes all 3 above test runners trip on the failing test, even without the use of `:include-src-dir`

```text
; clojure -Srepro -M:poly test project:workaround with:default
Projects to run tests from: workaround

Running tests for the workaround project using test runner: Polylith built-in clojure.test runner...
Running tests from the workaround project, including 3 bricks: has-test-paths, kaocha-wrapper-ext, src-only-workaround

Testing imrekoszo.poly-test-in-src.has-test-paths.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

Testing imrekoszo.poly-test-in-src.src-only-workaround.ifc

FAIL in (bad) (ifc.clj:6)
expected: (= :foo (bad))
  actual: (not (= :foo :bar))

Ran 1 tests containing 1 assertions.
1 failures, 0 errors.
```

```text
; clojure -Srepro -M:poly test project:workaround with:external
Projects to run tests from: workaround

Running tests for the workaround project using test runner: Polylith org.corfield.external-test-runner...
Running tests from the workaround project, including 3 bricks: has-test-paths, kaocha-wrapper-ext, src-only-workaround

Testing imrekoszo.poly-test-in-src.has-test-paths.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

Testing imrekoszo.poly-test-in-src.src-only-workaround.ifc

FAIL in (bad) (ifc.clj:6)
expected: (= :foo (bad))
  actual: (not (= :foo :bar))

Ran 1 tests containing 1 assertions.
1 failures, 0 errors.
Execution error at org.corfield.external-test-runner-cli.main/-main$fn (main.clj:134).

Test results: 0 passes, 1 failures, 0 errors.

Full report at:
/var/folders/sw/hg9d03hs7w50dfrhmxb4v6w00000gn/T/clojure-829537778640689272.edn
External test runner failed
```

```text
; clojure -Srepro -M:poly test project:workaround with:kaocha
Projects to run tests from: workaround

Running tests for the workaround project using test runner: polylith-kaocha...
[(.)(F)]
Randomized with --seed 1900119926

FAIL in imrekoszo.poly-test-in-src.src-only-workaround.ifc/bad (ifc.clj:6)
Expected:
  :foo
Actual:
  -:foo +:bar
2 tests, 2 assertions, 1 failures.
Tests failed [polylith-kaocha]
```

## Dummy test workaround

@seancorfield suggests another potential workaround in [polylith#448](https://github.com/polyfy/polylith/issues/448): create a dummy test in projects where all proper tests are under the src directories.

### Using this, none of the 3 test runners find the failing src test when `:include-src-dir` is not set

```text
; clojure -Srepro -M:poly test project:dummy with:default:external:kaocha
Projects to run tests from: dummy

Running tests for the dummy project using test runner: Polylith built-in clojure.test runner...
Running tests from the dummy project, including 3 bricks: dummy-test, has-test-paths, kaocha-wrapper-ext

Testing imrekoszo.poly-test-in-src.dummy-test.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

Testing imrekoszo.poly-test-in-src.has-test-paths.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.
Running tests for the dummy project using test runner: Polylith org.corfield.external-test-runner...
Running tests from the dummy project, including 3 bricks: dummy-test, has-test-paths, kaocha-wrapper-ext

Testing imrekoszo.poly-test-in-src.dummy-test.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

Testing imrekoszo.poly-test-in-src.has-test-paths.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.
Running tests for the dummy project using test runner: polylith-kaocha...
[(.)(.)]
2 tests, 2 assertions, 0 failures.

Execution time: 3 seconds
```

### But both external and kaocha find the failing test when `:include-src-dir` is set

(default doesn't)

```text
; clojure -Srepro -M:poly test project:dummy with:default:include-src-dir
Projects to run tests from: dummy

Running tests for the dummy project using test runner: Polylith built-in clojure.test runner...
Running tests from the dummy project, including 3 bricks: dummy-test, has-test-paths, kaocha-wrapper-ext

Testing imrekoszo.poly-test-in-src.dummy-test.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

Testing imrekoszo.poly-test-in-src.has-test-paths.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

Execution time: 1 seconds
```

```text
; clojure -Srepro -M:poly test project:dummy with:external:include-src-dir
Projects to run tests from: dummy

Test runner options:
  :include-src-dir => true

Running tests for the dummy project using test runner: Polylith org.corfield.external-test-runner...
Running tests from the dummy project, including 3 bricks: dummy-test, has-test-paths, kaocha-wrapper-ext

Testing imrekoszo.poly-test-in-src.dummy-test.ifc-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Test results: 1 passes, 0 failures, 0 errors.

Testing imrekoszo.poly-test-in-src.dummy-test.ifc

FAIL in (bad) (ifc.clj:6)
expected: (= :foo (bad))
  actual: (not (= :foo :bar))

Ran 1 tests containing 1 assertions.
1 failures, 0 errors.
Execution error at org.corfield.external-test-runner-cli.main/-main$fn (main.clj:134).

Test results: 0 passes, 1 failures, 0 errors.

Full report at:
/var/folders/sw/hg9d03hs7w50dfrhmxb4v6w00000gn/T/clojure-6345575265500441230.edn
External test runner failed
```

```text
; clojure -Srepro -M:poly test project:dummy with:kaocha:include-src-dir
Projects to run tests from: dummy

Running tests for the dummy project using test runner: polylith-kaocha...
[(.)(F)(.)]
Randomized with --seed 1186820747

FAIL in imrekoszo.poly-test-in-src.dummy-test.ifc/bad (ifc.clj:6)
Expected:
  :foo
Actual:
  -:foo +:bar
3 tests, 3 assertions, 1 failures.
Tests failed [polylith-kaocha]
```
