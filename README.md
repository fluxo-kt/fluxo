# Fluxo

![Stability: Alpha](https://kotl.in/badges/alpha.svg)
[![Snapshot Version](https://img.shields.io/badge/dynamic/xml?color=f68244&logo=gradle&labelColor=666&label=&query=%2F%2Fversion%5Blast%28%29%5D&url=https%3A%2F%2Fs01.oss.sonatype.org%2Fcontent%2Frepositories%2Fsnapshots%2Fio%2Fgithub%2Ffluxo-kt%2Ffluxo-core%2Fmaven-metadata.xml)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/fluxo-kt/fluxo-core)
[![Kotlin Version][badge-kotlin]][badge-kotlin-link]
[![Build](../../actions/workflows/build.yml/badge.svg)](../../actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/fluxo-kt/fluxo/branch/main/graph/badge.svg?token=LKCNVWR8QC)](https://codecov.io/gh/fluxo-kt/fluxo)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

![Kotlin Multiplatform][badge-kmp]
![JVM][badge-jvm] ![badge][badge-android]
![badge][badge-ios] ![badge][badge-mac] ![badge][badge-watchos] ![badge][badge-tvos]
![badge][badge-win] ![badge][badge-linux] ![badge][badge-js]

---

**Fluxo** *\[ˈfluksu]* is a simple yet super powerful state management library for Kotlin Multiplatform.

> Approach is best known as `BLoC`[^1], `MVI`[^2], `MVVM+`[^3], `Redux`[^4], SAM[^5], or even `State Machine`/`FSM`[^6].
Often used in the UI or presentation layers of the architecture.
But suitable and proven useful for any architectural layer of the app for any platform.

If you need predictable unidirectional data flow (`UDF`) or deterministic control over your state changes,
**Fluxo** will get you covered!

**Work-In-Progress**, first release is coming. **API isn't stable yet!**

### TLDR: Use SNAPSHOT artefact in Gradle (in a safe and reproducible way)
[![Latest snapshot](https://img.shields.io/badge/dynamic/xml?color=f68244&logo=gradle&label=Latest%20snapshot&query=%2F%2Fversion%5Blast%28%29%5D&url=https%3A%2F%2Fs01.oss.sonatype.org%2Fcontent%2Frepositories%2Fsnapshots%2Fio%2Fgithub%2Ffluxo-kt%2Ffluxo-core%2Fmaven-metadata.xml)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/fluxo-kt/fluxo-core/maven-metadata.xml)
<br>Select a snapshot for the preferred commit using a scheme: `0.1-<SHORT_COMMIT_SHA>-SNAPSHOT`.
<br>For example: `0.1-389443b-SNAPSHOT`

```kotlin
implementation("io.github.fluxo-kt:fluxo-core:0.1-<SHORT_COMMIT_SHA>-SNAPSHOT")
// For common data states
implementation("io.github.fluxo-kt:fluxo-data:0.1-<SHORT_COMMIT_SHA>-SNAPSHOT")
```
```kotlin
// in `settings.gradle.kts` of the project
repositories {
  maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}
```


### Code examples

MVVM+ container with lambda intents.
```kotlin
import kotlinx.coroutines.*
import kt.fluxo.core.*
import kotlin.coroutines.coroutineContext

suspend fun main() {
  // State here is an Int value. One-liner creation! Type inference is automatic!
  val container = container(initialState = 0)
  repeat(100) {
    container.intent {
      // `updateState {}` used to access the previous value safely.
      // Use `value = ...` if previous value isn't needed.
      updateState { prev -> prev + it }

      // suspendable lambda intent, it can start long-running side jobs and send side effects!
    }
  }

  // Listen to reactive state updates somewhere. Container is a `StateFlow`!
  // Supports cooperative cancellation on container close.
  CoroutineScope(coroutineContext).launch {
    container.collect(::println)
  }

  // You can wait for the full completion,
  // but it's optional, `container.close()` is also available.
  container.closeAndWait()
}
```

<details>
<summary>Strict Redux/MVI store with pure non-suspendable reducer.</summary>

```kotlin
import kt.fluxo.core.*

suspend fun main() {
  // Type inference is automatic if the reducer argument type is specified!
  val store = store<Int /* Intent */, Int /* State */>(
    initialState = 0,
    // pure non-suspendable reducer
    // `State.(Intent) -> State`
    reducer = { this + it }
  )
  repeat(100) {
    // emit is suspendable
    // use `send` methods for intents from non-suspend contexts.
    store.emit(it)
  }
}
```
</details>

<details>
<summary>Strict MVI intents with powerful MVVM+ DSL in suspendable handler.</summary>

```kotlin
import kt.fluxo.core.*

suspend fun main() {
  // Type inference is automatic if the handler argument type is specified!
  val store = store(
    initialState = 0,
    // suspendable handler, it can start long-running side jobs and send side effects!
    handler = { intent: Int ->
      updateState { prev -> prev + intent }
    }
  )
  // emit is suspendable
  // use `send` methods for intents from non-suspend contexts.
  repeat(100) { store.emit(it) }
}
```
</details>


## Overview

> **Fluxo** was started as a test for the hypothesis:
_it should be possible to combine all the strong sides of strict **Redux/MVI**[^4] with flexibility,
ease of readability, and maintainability of the **MVVM+**[^3]._
>
> The experiment paid off!
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
* **Multiplatform**, supports all KMP/KMM[^7] targets (**Android**, **iOS**, **JVM**,
  **JS**, **Linux**, **Windows/MinGW**, **macOS**, **watchOS**, **tvOS**).
* Different usage styles:
  * Strict **Redux/MVI**[^4] (the highest correctness guarantees, but may be subjectively less readable and intuitive)
  * Flexible **MVVM+**[^3] (intuitively readable, may be easier to maintain, has support for every feature and more :)
  * Redux-style discrete intents with MVVM+ style reduction DSL (hybrid way)
  * _More is coming…_
* Side jobs for long-running tasks (MVVM+ DSL).
  * For example, start a long-going task safely on intent. Jobs with the same name auto cancel earlier started ones.
  * Run something only when listeners are attached using [`repeatOnSubscription`][repeatOnSubscription].
  * Recover from any error within the side job with an `onError` handler.
* Bootstrap, kind of optional initialization side job that starts on the `Store` launch.
* **Side effect** support (sometimes called **news** or **one-off event**).
  * **Note that using side effects is generally considered as antipattern!**[^a][^b][^c].<br>
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
    once)[^e1][^e2].
* **Lifecycle-awareness** with full control based on coroutine scopes.
* Subscription lifecycle with convenience API ([`repeatOnSubscription`][repeatOnSubscription]).
  * Do something in store when subscriber connects or disconnects.
  * Listen to the number of subscribers with [`subscriptionCount`][subscriptionCount] `StateFlow`.
* Forceful customization:
  * Pluggable **intent strategies**:
    * _First In, First Out_ (Fifo). Default, predictable, and intuitive, ordered processing with good performance.
    * _Last In, First Out_ (Lifo). Can improve responsiveness, e.g. UI events processing, but may lose some intents!
    * _Parallel_. No processing order guarantees, can provide better performance and responsiveness compared to _Fifo_.
    * _Direct_. No pipeline. Immediately executes every intent until the first suspension point in the current thread.
    * _ChannelLifo_. Special `Channel`-based Lifo implementation that provides extra customization compared to _Lifo_.
    * Create your own!
  * Eager or lazy initialization of the store.
  * Global default settings for easier setup of state stores swarm.
    * Change settings once and for all at one place (`FluxoSettings.DEFAULT`).
    * Provide a prepared settings object for the group of your stores.
    * Or configure each store individually.
* Common data states in a [`fluxo-data`](fluxo-data) module *(Success, Failure, Loading, Cached, Empty, Not Loaded)*.
* Error handling and exception behavior control.
  * On the level of `StoreFactory` (for many `Store`s).
  * For each `Store`.
  * For the separate `sideJob`.
* Leak-free transfer, delivery guarantees[^el1][^el2] for intents and side effects.
* Complete interception for any possible event (like in OkHttp, etc.)
* Strictly not recommended, but JVM `Closeable` resources are experimentally supported as a state and side effects.
  * The previous state is closed on change.
  * Side effects are closed when not delivered.
  * However, no clear guarantees!
* Intentionally unopinionated, extensible API: you can follow guides or use it as you want.
* Well tested.
* Great performance.
  * See the [updated JMH benchmark results](../../actions/workflows/benchmark.yml), comparing different state-managing libs,
    e.g. [this one](../../actions/runs/4884981661#summary-13239135090).
* Reactive streams compatibility
  through [coroutine wrappers](https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive):
  * RxJava 2.x, RxJava 3.x
  * Flow (JDK 9), Reactive Streams
  * Project Reactor
* [LiveData](https://developer.android.com/topic/libraries/architecture/coroutines#livedata) compatibility with
  AndroidX.


### JMH Benchmark results

Compares the performance of different MVI or state-management libraries.<br>
Deep feature comparison is in-progress.
Write [me](https://t.me/samally) if you want early access!

> Single-thread simple incrementing intents (12 tests in 2 modes, [2023-05-04](../../actions/runs/4884981661#summary-13239135090)), [updates on CI](../../actions/workflows/benchmark.yml)

| Benchmark |       Mode       | Score | Units             | Percent |
|-----------|:----------------:|------:|:------------------|--------:|
| visualfsm__sm_reducer | <sub>thrpt</sub> | <b>5.043</b><sub><i> &#177; 0.159</i></sub> | <sub>ops/ms</sub> | <sub><i>0.0%</i></sub> |
| mvicore__mvi_reducer | <sub>thrpt</sub> | <b>4.774</b><sub><i> &#177; 0.250</i></sub> | <sub>ops/ms</sub> | <sub><i>-5.3%</i></sub> |
| fluxo__mvi_reducer | <sub>thrpt</sub> | <b>4.063</b><sub><i> &#177; 0.144</i></sub> | <sub>ops/ms</sub> | <sub><i>-19.4%</i></sub> |
| mvikotlin__mvi_reducer | <sub>thrpt</sub> | <b>1.921</b><sub><i> &#177; 0.273</i></sub> | <sub>ops/ms</sub> | <sub><i>-61.9%</i></sub> |
| fluxo__mvvmp_intent | <sub>thrpt</sub> | <b>1.350</b><sub><i> &#177; 0.185</i></sub> | <sub>ops/ms</sub> | <sub><i>-73.2%</i></sub> |
| fluxo__mvi_handler | <sub>thrpt</sub> | <b>1.302</b><sub><i> &#177; 0.034</i></sub> | <sub>ops/ms</sub> | <sub><i>-74.2%</i></sub> |
| genakureduce__mvi_handler | <sub>thrpt</sub> | <b>1.149</b><sub><i> &#177; 0.045</i></sub> | <sub>ops/ms</sub> | <sub><i>-77.2%</i></sub> |
| orbit__mvvmp_intent | <sub>thrpt</sub> | <b>0.576</b><sub><i> &#177; 0.087</i></sub> | <sub>ops/ms</sub> | <sub><i>-88.6%</i></sub> |
| ballast__mvi_handler | <sub>thrpt</sub> | <b>0.444</b><sub><i> &#177; 0.096</i></sub> | <sub>ops/ms</sub> | <sub><i>-91.2%</i></sub> |
| flowmvi__mvi_handler | <sub>thrpt</sub> | <b>0.385</b><sub><i> &#177; 0.073</i></sub> | <sub>ops/ms</sub> | <sub><i>-92.4%</i></sub> |
| flowredux__mvi_handler | <sub>thrpt</sub> | <b>0.104</b><sub><i> &#177; 0.006</i></sub> | <sub>ops/ms</sub> | <sub><i>-97.9%</i></sub> |
|  |                  |  |                   |  |
| mvicore__mvi_reducer | <sub>avgt</sub>  | <b>0.522</b><sub><i> &#177; 0.016</i></sub> | <sub>ms/op</sub>  | <sub><i>0.0%</i></sub> |
| visualfsm__sm_reducer | <sub>avgt</sub>  | <b>0.595</b><sub><i> &#177; 0.025</i></sub> | <sub>ms/op</sub>  | <sub><i>14.0%</i></sub> |
| fluxo__mvi_reducer | <sub>avgt</sub>  | <b>0.627</b><sub><i> &#177; 0.014</i></sub> | <sub>ms/op</sub>  | <sub><i>20.1%</i></sub> |
| mvikotlin__mvi_reducer | <sub>avgt</sub>  | <b>1.549</b><sub><i> &#177; 0.058</i></sub> | <sub>ms/op</sub>  | <sub><i>196.7%</i></sub> |
| fluxo__mvi_handler | <sub>avgt</sub>  | <b>2.101</b><sub><i> &#177; 0.055</i></sub> | <sub>ms/op</sub>  | <sub><i>302.5%</i></sub> |
| genakureduce__mvi_handler | <sub>avgt</sub>  | <b>2.183</b><sub><i> &#177; 0.038</i></sub> | <sub>ms/op</sub>  | <sub><i>318.2%</i></sub> |
| fluxo__mvvmp_intent | <sub>avgt</sub>  | <b>2.236</b><sub><i> &#177; 0.189</i></sub> | <sub>ms/op</sub>  | <sub><i>328.4%</i></sub> |
| orbit__mvvmp_intent | <sub>avgt</sub>  | <b>4.938</b><sub><i> &#177; 0.231</i></sub> | <sub>ms/op</sub>  | <sub><i>846.0%</i></sub> |
| ballast__mvi_handler | <sub>avgt</sub>  | <b>5.269</b><sub><i> &#177; 0.178</i></sub> | <sub>ms/op</sub>  | <sub><i>909.4%</i></sub> |
| flowmvi__mvi_handler | <sub>avgt</sub>  | <b>6.928</b><sub><i> &#177; 0.086</i></sub> | <sub>ms/op</sub>  | <sub><i>1227.2%</i></sub> |
| flowredux__mvi_handler | <sub>avgt</sub>  | <b>26.605</b><sub><i> &#177; 1.190</i></sub> | <sub>ms/op</sub>  | <sub><i>4996.7%</i></sub> |


### Roadmap

* [ ] Support new Kotlin `AutoCloseable` interface
  ([since Kotlin 1.8.20](https://kotlinlang.org/docs/whatsnew-eap.html#experimental-support-for-autocloseable-interface-in-standard-library),
  [Android API level 19](https://developer.android.com/reference/java/lang/AutoCloseable))
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

### Heavily inspired by

* [Ballast](https://github.com/copper-leaf/ballast)
* [Orbit MVI](https://github.com/orbit-mvi/orbit-mvi)
* [MVIKotlin](https://github.com/arkivanov/MVIKotlin)

### Versioning

**Fluxo** uses [SemVer](http://semver.org/) for versioning.
For the versions available, see the [tags on this repository](../../tags).

### Code quality checks

[![CodeFactor](https://www.codefactor.io/repository/github/fluxo-kt/fluxo/badge/main)](https://www.codefactor.io/repository/github/fluxo-kt/fluxo/overview/main)
[![CodeBeat](https://codebeat.co/badges/5ed83de6-f399-4880-9a94-d42d1ab43b89)](https://codebeat.co/projects/github-com-fluxo-kt-fluxo-main)
[![Codacy](https://app.codacy.com/project/badge/Grade/ea7dfbbaf83441eea468f4f083604280)](https://www.codacy.com/gh/fluxo-kt/fluxo/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=fluxo-kt/fluxo&amp;utm_campaign=Badge_Grade)
[![Sonatype Lift](https://img.shields.io/badge/Sonatype-Lift-green)](https://lift.sonatype.com/results/github.com/fluxo-kt/fluxo)
<br>
[![CodeClimate](https://api.codeclimate.com/v1/badges/af292519a2481f9a47a6/maintainability)](https://codeclimate.com/github/fluxo-kt/fluxo-mvi/maintainability)

### License

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

This project is licensed under the Apache License, Version 2.0 — see the
[license](LICENSE) file for details.


[^1]: BLoC stands for [Business Logic Components](https://web.archive.org/web/20221205073553/https://www.didierboelens.com/2018/08/reactive-programming-streams-bloc/),
architectural pattern [[1](https://soshace.com/understanding-flutter-bloc-pattern/), [2](https://bloclibrary.dev/)]
[^2]: MVI: [Model-View-Intent](http://hannesdorfmann.com/android/model-view-intent/) architectural pattern.
[^3]: [MVVM+, orbit-way][orbit-mvvm+]: updated [Model-View-ViewModel](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) pattern,
aka Redux/MVI with [contextual reduction][contextual-reduction].
[^4]: Redux: Pattern for predictable managing and updating app state, and [a famous library](https://redux.js.org/).
[^5]: SAM: [State-Action-Model](https://sam.js.org/) architectural pattern.
[^6]: FSM: [Finite-State Machine](https://en.wikipedia.org/wiki/Finite-state_machine).
[^7]: [KMP/KMM](https://kotlinlang.org/lp/mobile/): Kotlin Multiplatform, Kotlin Multiplatform for mobile.

[^a]: [ViewModel: One-off event antipatterns](https://medium.com/androiddevelopers/viewmodel-one-off-event-antipatterns-16a1da869b95)
(2022, by Manuel Vivo from Google)
[^b]: [Google Guide to app architecture, UI events > Other use cases > Note](https://developer.android.com/topic/architecture/ui-layer/events#other-use-cases) (Apr 2023)
[^c]: [How To Handle ViewModel One-Time Events In Jetpack Compose, One-Time-Event Anti-Pattern](https://proandroiddev.com/how-to-handle-viewmodel-one-time-events-in-jetpack-compose-a01af0678b76#0009)] (2022, by Yanneck Reiß)

[^e1]: \[Proposal] Primitive or Channel that guarantees the delivery and processing of items (Kotlin/kotlinx.coroutines#2886)
[^e2]: [SingleLiveEvent case with an Event wrapper](https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150#0e87).

[^el1]: Leak-free closeable resources transfer via Channel (Kotlin/kotlinx.coroutines#1936)
[^el2]: [Unfortunately events may be dropped from `kotlinx.coroutines` Channel](https://gmk57.medium.com/unfortunately-events-may-be-dropped-if-channel-receiveasflow-cfe78ae29004)
[^el3]: [Migrate from LiveData to Flow > Hints](https://github.com/EventFahrplan/EventFahrplan/issues/519)


[Store]: fluxo-core/src/commonMain/kotlin/kt/fluxo/core/Store.kt
[repeatOnSubscription]: fluxo-core/src/commonMain/kotlin/kt/fluxo/core/FluxoDsl.kt#L50
[subscriptionCount]: fluxo-core/src/commonMain/kotlin/kt/fluxo/core/Store.kt#L55

[StateFlow]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/
[FlowCollector]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow-collector/
[CoroutineScope]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/

[orbit-mvvm+]: https://github.com/orbit-mvi/orbit-mvi/blob/6b6f290/README.md#what-is-orbit
[contextual-reduction]: https://dev.to/feresr/a-case-against-the-mvi-architecture-pattern-1add

[badge-kotlin]: http://img.shields.io/badge/Kotlin-1.8.21-7F52FF?logo=kotlin&logoColor=7F52FF&labelColor=2B2B2B
[badge-kotlin-link]: https://github.com/JetBrains/kotlin/releases

[badge-kmp]: http://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=7F52FF&labelColor=2B2B2B
[badge-jvm]: http://img.shields.io/badge/-JVM-530E0E?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAMAAAAolt3jAAAAh1BMVEUAAABTgqFTgJ9Yg6VTgqFSg6FUgaFTgZ9TgqFSg6FSgqJTgp/ncABVgqRVgKpTg6HnbwDnbwBTgqDnbwDocADocADnbwDnbQBOgJ1Vg6T/ZgDnbwDnbwDnbwBTgqHnbwBTgqBTgqJTgaDnbgDnbwDnbgBVgqFRgqNRgKLpbQDpcQDjcQDtbQD42oiEAAAALXRSTlMAQyEPSWlUJqlwXllQKgaijIRmYFY3Lx8aEwXz5dLEta+ZlHZsQTkvLCMiEg6oPAWiAAAAfklEQVQI102LVxKDMBBDtbsugDGdUNJ7vf/5MjAJY33pjfTwz2eFMOk1pLrAuMBzX7fN8m63XXkJvAKbJjDPXbp+/3rmBa+xBIQY4EhXxiSKLLCbZn1n9qQy0WrC3pkqUYx+VgebH/MomhecDsqyyMAPopsA4p2O4zhxhsh+ASqXBd13PdMrAAAAAElFTkSuQmCC
[badge-android]: https://img.shields.io/badge/-Android-0E3B1A?logo=android&logoColor=3DDC84

[badge-ios]: http://img.shields.io/badge/-iOS-E5E5EA?logo=apple&logoColor=64647D
[badge-mac]: http://img.shields.io/badge/-macOS-F4F4F4?logo=apple&logoColor=6D6D88
[badge-watchos]: http://img.shields.io/badge/-watchOS-C0C0C0?logo=apple&logoColor=4C4C61
[badge-tvos]: http://img.shields.io/badge/-tvOS-808080?logo=apple&logoColor=23232E

[badge-win]: http://img.shields.io/badge/-Windows-00ADEF?logo=windows&logoColor=FCFDFD
[badge-linux]: http://img.shields.io/badge/-Linux-6E1F7C?logo=linux&logoColor=FFF6DB
[badge-js]: http://img.shields.io/badge/-JavaScript-F8DB5D?logo=javascript&logoColor=312C02
