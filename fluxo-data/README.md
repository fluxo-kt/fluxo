## Common data utils for Fluxo

* Wrapper [`FluxoData`](src/commonMain/kotlin/kt/fluxo/data/FluxoData.kt) type for common **Async/UI/Repository/Cached data states**.
  Can represent state in a mixed condition, like *"cached empty value"* or *"loading with some default data"*, etc.
  * Not loaded
  * Cached
  * Loading
  * Empty
  * Success
  * Failure

### Roadmap

* [ ] `FluxoData` Mapping and handling utils.
* [ ] Fetching utils for easier `FluxoData` usage.
  * [ ] \(Optional) Retry logic.

### Inspired and based on ideas from:

* [Kotlin Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)
* [Ballast Cached](https://copper-leaf.github.io/ballast/wiki/modules/ballast-repository/#cached)
* [Uniflow UIState](https://github.com/uniflow-kt/uniflow-kt/blob/2a8835b/uniflow-core/src/main/kotlin/io/uniflow/core/flow/data/UIState.kt#L25)
