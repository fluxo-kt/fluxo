# Fluxo MVI / MVVM+ Multiplatform

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.md)

**Fluxo** is a simple yet super powerful MVVM+ / MVI library for Kotlin Multiplatform.

* Kotlin **coroutine-based** state handling.
* **Multiplatform**, supports all KMM targets (**Android**, **iOS**, **JVM**, **JS**, **Native**..).
* Simple usage, type-safe, no-boilerplate.
* Different usage styles:
  * Strict **Redux/MVI**
  * Simple and flexible **MVVM+**
    ([contextual reduction](https://dev.to/feresr/a-case-against-the-mvi-architecture-pattern-1add)
    , [orbit-way](https://github.com/orbit-mvi/orbit-mvi#what-is-orbit))
  * Redux-style discrete Inputs with MVVM+ style reduction DSL
* **Side effects** support (sometimes called news or events).
  * Four strategies allows to fully control how side effects sharing can be handled in the Store
    (RECEIVE, CONSUME, SHARE, DISABLE).
  * Side effects are cached while subscriber (e.g., view) is not attached.
  * Side effects consumption guarantees with `GuaranteedEffect` (effect handled and exactly
    once) [[1](https://github.com/Kotlin/kotlinx.coroutines/issues/2886),
    [2](https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)]
    .
* **Lifecycle-aware** with full control based on coroutine scopes.
* Pluggable **input strategies**:
  * First In, First Out (Fifo). Default, best for background processing.
  * Last In, First Out (Lifo). Can optimize UI events processing.
  * Parallel, no order guarantees.
  * Create your own!
* Bootstrap (initialization tasks), eager or lazy initialization.
* Side jobs for long-running tasks (MVVM+ DSL).
* Errors handling and on exception behavior control.
* Leak-free transfer, delivery
  guarantees [[1](https://github.com/Kotlin/kotlinx.coroutines/issues/1936), [2](https://gmk57.medium.com/unfortunately-events-may-be-dropped-if-channel-receiveasflow-cfe78ae29004)]
  .
* Strictly not recommended, but JVM `Closeable` resources partially supported as a state and side effects.
  * Previous state will be properly closed on change.
  * Side effects closed when not delivered.
* Intentionally unopinionated, extensible API: you can follow guides or use it as you want.
* Well tested.
* Reactive streams compatibility
  through [coroutine wrappers](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive):
  * RxJava 2.x, RxJava 3.x
  * Flow (JDK 9), Reactive Streams
  * Project Reactor
* [LiveData](https://developer.android.com/topic/libraries/architecture/coroutines#livedata) compatibility with AndroidX.

### Roadmap

- [ ] Subscription Lifecycle
- [ ] Complete code coverage with tests
  - [ ] SideJobs tests
  - [ ] Input strategy tests
  - [ ] Reducer tests
  - [ ] MVI Store tests
  - [ ] Bootstrapper tests
  - [ ] Side effects strategies/cache/guarantees tests
- [ ] \(Optional) Java-friendly API
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

