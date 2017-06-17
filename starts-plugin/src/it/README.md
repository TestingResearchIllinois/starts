To run these integration tests (ITs), run `mvn integration-test`.

Existing integration tests check some basic usage scenarios of
STARTS. See <description> tags in each IT for explanation of what it
tests.

To add more tests, refer to `first-it`, which provides a basic usage
of `verify.goovy` to check IT output and `setup.groovy` to change
source code between consecutive runs of STARTS.

TODO (ITs to add)
-----------------

* change non-java file between consecutive runs
* use more complicated dependency among the project's tests and
  classes
* check intermediate results of starts such as the graph, dependencies
  etc
* tests on the different STARTS options
* check that .clz and .zlc find the same tests
* check that when reflective edges are available, STARTS selects tests
  correctly
