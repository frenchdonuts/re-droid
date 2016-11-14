# re-droid #

A reactive state container written on top of RxJava and based on Clojurescript's [re-frame](https://github.com/Day8/re-frame) library. This is mainly for use in Android development.

## Notes

For the utility classes under [utils/](https://bitbucket.org/frenchdonuts/re-droid/src/c07de358e8b6f4130a6af3a265230e2e34be9b8d/library/src/main/kotlin/io/onedonut/re_droid/utils/?at=master) to work properly, make sure you are using only immutable data structures. So Kotlin's data classes and maybe [dex](https://github.com/andrewoma/dexx) for immutable collections.