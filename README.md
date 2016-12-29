# re-droid #

A reactive state container written on top of RxJava and based on Clojurescript's [re-frame](https://github.com/Day8/re-frame) library. This is mainly for use in Android development.

## TODO

1. Use rxKotlin's distinctUntilChanged operator instead of manually comparing the previous query result.
2. Add tests to see if dispatching an action inside of an RxAction leads to an inconsistent state. If so, fix it.

## Notes

For the utility classes under [utils/](https://bitbucket.org/frenchdonuts/re-droid/src/c07de358e8b6f4130a6af3a265230e2e34be9b8d/library/src/main/kotlin/io/onedonut/re_droid/utils/?at=master) to work properly, make sure you are using only immutable data structures. So Kotlin's data classes and maybe [dex](https://github.com/andrewoma/dexx) for immutable collections.
