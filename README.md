# Fluxo MVI / MVVM+ Multiplatform

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.md)

**Fluxo** is a simple yet super powerful MVI / MVVM+ library for Kotlin Multiplatform.

* Kotlin **coroutine-based** state handling.
* **Multiplatform**, supports all KMM targets (**Android**, **iOS**, **JVM**, **JS**, **Native**..).
* Simple, type-safe, no-boilerplate.
* Different usage styles:
  * Strict **Redux/MVI**
  * Flexible **MVVM+** ([contextual reduction](https://dev.to/feresr/a-case-against-the-mvi-architecture-pattern-1add))
  * Redux-style discrete Inputs with MVVM+ style DSL
* **Side effects** support (sometimes called news or events)
* Intentionally unopinionated, extensible API: you can follow guides or use it as you want.
* **Lifecycle-aware** with full control based on coroutine scopes.
* Pluggable **input strategies**:
  * First In, First Out (Fifo), default, best for background processing.
  * Last In, First Out (Lifo), best for UI events processing.
  * Parallel, no order guarantees.
* Full errors handling and behavior control.
* Bootstrap for eager or lazy initialization.
* Side jobs for long-running tasks.
* Well tested.
* Reactive streams compatibility
  through [coroutine wrappers](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive):
  * RxJava 2.x, RxJava 3.x
  * Reactive Streams, Flow (JDK 9)
  * Project Reactor
  * [LiveData](https://developer.android.com/topic/libraries/architecture/coroutines#livedata)

### Roadmap

- [ ] Subscription Lifecycle
- [ ] Side effects strategies (see `ActionShareBehavior` in FlowMVI)
- [ ] Side effects cache when view not attached
- [ ] Side effects consumption
  control ([1](https://proandroiddev.com/how-to-handle-viewmodel-one-time-events-in-jetpack-compose-a01af0678b76#0009)
  , [2](https://medium.com/androiddevelopers/viewmodel-one-off-event-antipatterns-16a1da869b95))
- [ ] Complete code coverage with tests
  - [ ] SideJobs tests
  - [ ] Input strategy tests
  - [ ] Reducer tests
  - [ ] MVI Store tests
  - [ ] Bootstrapper tests
- [ ] Compose integration tests, examples and docs
- [ ] ViewModel integration tests, examples and docs
- [ ] SavedState (android state preservation) integration tests, examples and docs
- [ ] [Essenty](https://github.com/arkivanov/Essenty) integration tests, examples and docs
- [ ] Compose Desktop (JetBrains) integration tests, examples and docs
- [ ] [SAM: State-Action-Model](https://sam.js.org/), composable
  - [ ] functions as first-class citizens
- [ ] Store to Store connection
- [ ] FSM: Strict finite-state machine style with edges declaration
- [ ] Unit test library
- [ ] Espresso idling resource support
- [ ] Rx* libraries integration tests, examples and docs
- [ ] LiveData integration tests, examples and docs
- [ ] Complete pipeline interception (as with OkHttp, etc.)
- [ ] Time-travel (MviKotlin, Ballast, Flipper integration)
- [ ] Logging module
- [ ] DI support tests, examples and docs
- [ ] SideJobs registry/management API
- [ ] [State graph tools](https://github.com/Kontur-Mobile/VisualFSM#tools-of-visualfsm)
  - [ ] Get unreachable states, build an Edge List, build states Adjacency Map.
  - [ ] App containers aggregation for graph tools
- [ ] Documentation and examples
- [ ] [Partial state change with effect](https://github.com/uniflow-kt/uniflow-kt/blob/master/doc/notify_update.md)
- [ ] [Arrow](https://arrow-kt.io/) integration tests, examples and docs
- [ ] Common Async/UI/Repository/Cached states
- [ ] Debug checks
- [ ] Analytics/Crashlytics integration
- [ ] \(Optional) Undo/Redo
- [ ] \(Optional) Stores synchronization

## Based on architectural ideas:

- [MVI: Model-View-Intent](http://hannesdorfmann.com/android/model-view-intent/)
- [MVVM: Model-View-ViewModel](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel)
- [FSM: Finite-state machine](https://en.wikipedia.org/wiki/Finite-state_machine)
- [SAM: State-Action-Model](https://sam.js.org/)

## Inspired and based on:

- [Ballast](https://github.com/copper-leaf/ballast)
- [Orbit MVI](https://github.com/orbit-mvi/orbit-mvi)

## Versioning

**Fluxo** uses [SemVer](http://semver.org/) for versioning. For the versions
available, see the [tags on this repository](/tags).

## License

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.md)

This project is licensed under the Apache License, Version 2.0 - see the
[license](LICENSE.md) file for details

