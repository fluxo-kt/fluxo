## Common data utils for Fluxo

![Stability: Beta](https://kotl.in/badges/beta.svg)
[![Snapshot Version](https://img.shields.io/badge/dynamic/xml?color=f68244&logo=gradle&labelColor=666&label=&query=%2F%2Fversion%5Blast%28%29%5D&url=https%3A%2F%2Fs01.oss.sonatype.org%2Fcontent%2Frepositories%2Fsnapshots%2Fio%2Fgithub%2Ffluxo-kt%2Ffluxo-data%2Fmaven-metadata.xml)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/fluxo-kt/fluxo-data)
[![codecov](https://codecov.io/gh/fluxo-kt/fluxo/branch/main/graph/badge.svg?token=LKCNVWR8QC)](https://app.codecov.io/gh/fluxo-kt/fluxo/tree/main/fluxo-data)

* Wrapper [`FluxoResult`](src/commonMain/kotlin/kt/fluxo/data/FluxoResult.kt) type for common
  **Async/UI/Repository/Cached states**. <br>Can represent state in a mixed condition, like *"cached empty value"*
  or *"loading with some default data"*, etc.
  * Not loaded
  * Cached
  * Loading
  * Empty
  * Success
  * Failure
* Mapping and handling util functions for `FluxoResult`.

### Roadmap

* [ ] Compile with JetBrains Compose for safe usage with Compose.
* [ ] Fetching utils for easier `FluxoResult` usage.
  * [ ] \(Optional) Retry logic.

### Inspired and based on ideas from:

* [Kotlin Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)
* [Ballast Cached](https://copper-leaf.github.io/ballast/wiki/modules/ballast-repository/#cached)
* [Uniflow UIState](https://github.com/uniflow-kt/uniflow-kt/blob/2a8835b/uniflow-core/src/main/kotlin/io/uniflow/core/flow/data/UIState.kt#L25)
* [ResultOf from Seanghay](https://github.com/seanghay/result-of), [[1]](https://medium.com/swlh/kotlin-sealed-class-for-success-and-error-handling-d3054bef0d4e)
