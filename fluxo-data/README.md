## Common data utils for Fluxo

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
