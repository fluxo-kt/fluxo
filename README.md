# Fluxo

![Stability: Alpha](https://kotl.in/badges/alpha.svg)
[![Snapshot Version](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/io.github.fluxo-kt/fluxo-core.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/fluxo-kt/)
[![Build](../../actions/workflows/build.yml/badge.svg)](../../actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/fluxo-kt/fluxo-mvi/branch/main/graph/badge.svg?token=LKCNVWR8QC)](https://codecov.io/gh/fluxo-kt/fluxo-mvi)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

![badge][badge-android]
![badge][badge-jvm]
![badge][badge-ios]
![badge][badge-tvos]
![badge][badge-watchos]
![badge][badge-mac]
![badge][badge-win]
![badge][badge-linux]
![badge][badge-js]

---

**Fluxo** *\[ˈfluksu]* is a simple yet super powerful state management library for Kotlin Multiplatform.

Approach is best known as `MVI`, `MVVM+`, `Redux`, or even `StateMachine`.
Often used in the UI or presentation layers of the architecture.
But suitable and proven useful for any layer of the app
if you need better control over your state changes and a structured pipeline for it!

**Work-In-Progress**, first release is coming. **API isn't stable yet!**

### TLDR: Use SNAPSHOT artefact in Gradle (in a safe and reproducible way)

Select a snapshot for the preferred commit using a scheme: `0.1-<SHORT_COMMIT_SHA>-SNAPSHOT`.
<br>For example: `0.1-f2b5944-SNAPSHOT`

```kotlin
dependencies {
  implementation("io.github.fluxo-kt:fluxo-core:0.1-<SHORT_COMMIT_SHA>-SNAPSHOT")
  // For common data states
  implementation("io.github.fluxo-kt:fluxo-data:0.1-<SHORT_COMMIT_SHA>-SNAPSHOT")
}
repositories {
  maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
}
```

## Overview

**Fluxo** was started as a test for the hypothesis:
_it should be possible to combine all the strong sides of strict **Redux/MVI** with flexibility,
ease of readability, and maintainability of the **MVVM+**._

The experiment paid off!
It is possible to combine **MVVM+** with great performance, high-quality time-travel, logging,
auto analysis of the transition graph and much more.
A long list of features is implemented gradually in this library (see the [Roadmap](#roadmap) for details).

Basic usage is elementary, yet you can take advantage of fine-tuning and super powerful features when you need them.

* Kotlin **coroutine-based** state container.
* One-liner creation, simple usage, type-safe, no-boilerplate!
* Use Fluxo for the UI, business, or data tasks with the same ease.
* Native integration with coroutines:
  * Each Fluxo [`Store`][Store] is a [`StateFlow`][StateFlow] with **states** and a [`FlowCollector`][FlowCollector]
    for **intents**.
    You can easily combine stores with each other and with any other flows, flow operators, and collectors.
  * Also, Fluxo [`Store`][Store] is a [`CoroutineScope`][CoroutineScope] itself, so you can integrate it with
    any existing coroutine workflow and treat just as a usual coroutine scope.
* **Multiplatform**, supports all Kotlin MPP/KMM targets (**Android**, **iOS**, **JVM**,
  **JS**, **Linux**, **Windows/MinGW**, **macOS**, **watchOS**, **tvOS**).
* Different usage styles:
  * Strict **Redux/MVI** (the highest correctness guarantees, but may be subjectively less readable and intuitive)
  * Flexible **MVVM+**
    (see [contextual reduction](https://dev.to/feresr/a-case-against-the-mvi-architecture-pattern-1add),
    [orbit-way](https://github.com/orbit-mvi/orbit-mvi#what-is-orbit), intuitively readable, may be easier
    to maintain, support for every feature and more :)
  * Redux-style discrete intents with MVVM+ style reduction DSL (hybrid way)
  * _More is coming…_
* **Side effect** support (sometimes called **news** or **one-off event**).
  * **Note that using side effects is generally considered as antipattern!**
    [[1](https://medium.com/androiddevelopers/viewmodel-one-off-event-antipatterns-16a1da869b95),
    [2](https://developer.android.com/topic/architecture/ui-layer/events#other-use-cases),
    [3](https://proandroiddev.com/how-to-handle-viewmodel-one-time-events-in-jetpack-compose-a01af0678b76#0009)].<br>
    But it can still be useful sometimes, especially when migrating an old codebase.<br>
    *However, if you find yourself in one of these situations,
    reconsider what this one-time event actually means for your app.
    Handle events immediately and reduce them to state.
    State is a better representation of the given point in time,
    and it gives you more delivery and processing guarantees.
    State is usually easier to test, and it integrates consistently with the rest of your app.*
  * Fluxo has four strategies to fully control how side effects are shared from the store
    (_RECEIVE_, _CONSUME_, _SHARE_, _DISABLE_).
  * Side effects are cached while the subscriber (e.g., view) isn't attached.
  * Side effects can have consumption guarantees with `GuaranteedEffect` (effect handled and exactly
    once) [[1](https://github.com/Kotlin/kotlinx.coroutines/issues/2886),
    [2](https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)].
* **Lifecycle-awareness** with full control based on coroutine scopes.
* Subscription lifecycle with convenience API (`repeatOnSubscription`). Do something in store when subscriber connects or disconnects.
* Forceful customization:
  * Pluggable **intent strategies**:
    * _First In, First Out_ (Fifo). Default, predictable, and intuitive, ordered processing with good performance.
    * _Last In, First Out_ (Lifo). Can improve responsiveness, e.g. UI events processing, but may lose some intents!
    * _Parallel_. No processing order guarantees, can provide better performance and responsiveness compared to _Fifo_.
    * _Direct_. No pipeline. Immediately executes every intent until the first suspension point in the current thread.
    * _ChannelLifo_. Special `Channel`-based Lifo implementation that provides extra customization compared to _Lifo_.
    * Create your own!
  * Eager or lazy initialization of the store.
  * Error handling and exception behavior control.
  * Global default settings for easier setup of state stores swarm.
    * Change settings once and for all at one place (`FluxoSettings.DEFAULT`).
    * Provide a prepared settings object for the group of your stores.
    * Or configure each store individually.
* Common data states in a [`fluxo-data`](fluxo-data) module *(Success, Failure, Loading, Cached, Empty, Not Loaded)*.
* Side jobs for long-running tasks (MVVM+ DSL).
* Bootstrap (initialization side job).
* Leak-free transfer, delivery
  guarantees [[1](https://github.com/Kotlin/kotlinx.coroutines/issues/1936), [2](https://gmk57.medium.com/unfortunately-events-may-be-dropped-if-channel-receiveasflow-cfe78ae29004)] for intents and side effects.
* Strictly not recommended, but JVM `Closeable` resources partially supported as a state and side effects.
  * The previous state will be closed on change.
  * Side effects closed when not delivered.
  * However, no clear guarantees!
* Intentionally unopinionated, extensible API: you can follow guides or use it as you want.
* Well tested.
* Reactive streams compatibility
  through [coroutine wrappers](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive):
  * RxJava 2.x, RxJava 3.x
  * Flow (JDK 9), Reactive Streams
  * Project Reactor
* [LiveData](https://developer.android.com/topic/libraries/architecture/coroutines#livedata) compatibility with
  AndroidX.

### Roadmap

* Support new Kotlin 1.8.20 `AutoCloseable`
  * https://kotlinlang.org/docs/whatsnew-eap.html#experimental-support-for-autocloseable-interface-in-standard-library
* [ ] Complete pipeline interception (as with OkHttp, etc.)
* [ ] sideJob helpers for logic called on a specific state or side effect (see _Kotlin-Bloc_).
* [ ] [SAM: State-Action-Model](https://sam.js.org/), composable
  * [ ] functions as first-class citizens
* [ ] Store to Store connection
* [ ] FSM: Strict finite-state machine style with edges declaration
* [ ] SideJobs registry/management API
* [ ] [Partial state change with effect](https://github.com/uniflow-kt/uniflow-kt/blob/master/doc/notify_update.md)
* [ ] Debug checks
* [ ] \(Optional) Java-friendly API
* [ ] Compose integration tests, examples and docs
* [ ] ViewModel integration tests, examples and docs
* [ ] SavedState (android state preservation) integration tests, examples and docs
* [ ] [Essenty](https://github.com/arkivanov/Essenty) integration tests, examples and docs
* [ ] Compose Desktop (JetBrains) integration tests, examples and docs
* [ ] Rx* libraries integration tests, examples and docs
* [ ] LiveData integration tests, examples and docs
* [ ] [Arrow](https://arrow-kt.io/) integration tests, examples and docs
* [ ] Time-travel (MviKotlin, Ballast, Flipper integration)
* [ ] Logging module
* [ ] Unit test library
  * [ ] Espresso idling resource support
* [ ] DI support tests, examples and docs
* [ ] JS/TS usage examples and NPM publication
* [ ] [State graph tools](https://github.com/Kontur-Mobile/VisualFSM#tools-of-visualfsm)
  * [ ] Get unreachable states, build an Edge List, build states Adjacency Map.
  * [ ] App containers aggregation for graph tools
* [ ] Analytics/Crashlytics integration
* [ ] Orbit, MVI Kotlin, etc. migration examples and tests for migration helpers.
* [ ] Documentation and examples
* [ ] \(Optional) Undo/Redo
* [ ] \(Optional) Stores synchronization

### Based on architectural ideas:

* [MVI: Model-View-Intent](http://hannesdorfmann.com/android/model-view-intent/)
* [MVVM: Model-View-ViewModel](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel)
* [FSM: Finite-state machine](https://en.wikipedia.org/wiki/Finite-state_machine)
* [SAM: State-Action-Model](https://sam.js.org/)

### Inspired and based on ideas from:

* [Ballast](https://github.com/copper-leaf/ballast)
* [Orbit MVI](https://github.com/orbit-mvi/orbit-mvi)
* [MVIKotlin](https://github.com/arkivanov/MVIKotlin)

### Versioning

**Fluxo** uses [SemVer](http://semver.org/) for versioning.
For the versions available, see the [tags on this repository](../../tags).

### Code quality checks

[![CodeFactor](https://www.codefactor.io/repository/github/fluxo-kt/fluxo-mvi/badge/main)](https://www.codefactor.io/repository/github/fluxo-kt/fluxo-mvi/overview/main)
[![CodeBeat](https://codebeat.co/badges/5ed83de6-f399-4880-9a94-d42d1ab43b89)](https://codebeat.co/projects/github-com-fluxo-kt-fluxo-mvi-main)
[![Codacy](https://app.codacy.com/project/badge/Grade/ea7dfbbaf83441eea468f4f083604280)](https://www.codacy.com/gh/fluxo-kt/fluxo-mvi/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=fluxo-kt/fluxo-mvi&amp;utm_campaign=Badge_Grade)
[![Sonatype Lift](https://img.shields.io/badge/Sonatype-Lift-green)](https://lift.sonatype.com/results/github.com/fluxo-kt/fluxo-mvi)
<br>
[![CodeClimate](https://api.codeclimate.com/v1/badges/af292519a2481f9a47a6/maintainability)](https://codeclimate.com/github/fluxo-kt/fluxo-mvi/maintainability)

### License

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

This project is licensed under the Apache License, Version 2.0 — see the
[license](LICENSE) file for details.


[Store]: fluxo-core/src/commonMain/kotlin/kt/fluxo/core/Store.kt
[StateFlow]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/
[FlowCollector]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow-collector/
[CoroutineScope]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/

[badge-android]: https://img.shields.io/badge/-android-6EDB8D.svg?logo=kotlin
[badge-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg?logo=kotlin

[badge-ios]: http://img.shields.io/badge/-ios-E5E5EA.svg?logo=kotlin
[badge-mac]: http://img.shields.io/badge/-macos-F4F4F4.svg?logo=kotlin
[badge-tvos]: http://img.shields.io/badge/-tvos-808080.svg?logo=kotlin
[badge-watchos]: http://img.shields.io/badge/-watchos-C0C0C0.svg?logo=kotlin

[badge-linux]: http://img.shields.io/badge/-linux-6E1F7C.svg?logo=kotlin
[badge-win]: http://img.shields.io/badge/-windows-00ADEF.svg?logo=kotlin
[badge-js]: http://img.shields.io/badge/-js-F8DB5D.svg?logo=kotlin
