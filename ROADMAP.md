
## Roadmap

* [ ] **Restore static analysis (high priority).** Detekt, Spotless and the harness lint setup are off:
  fluxo-kmp-conf enables them only with `setupVerification = true`, which the root build never sets
  (`enableSpotless = true` is inert without it). With the flag on, Spotless reformats a large share of the sources,
  detekt reports several hundred findings (mostly fluxo-core common code), and the Android detekt tasks fail variant
  resolution with fluxo-kmp-conf 0.15.1 on AGP 9 (a harness defect). Plan: one reformat commit, fix real findings,
  baseline the rest with reasons, enable Android detekt with the first fluxo-kmp-conf release that fixes it.
* [ ] **Activate the TypeScript-declaration API lane.** `tsApiChecks` passes vacuously and `api/js/*.d.ts` is stale
  (AGENTS.md gotcha #5). Needs a fluxo-bcv-js release containing commit be5369c; then bump it and review the first
  regenerated `.d.ts` dumps hunk by hunk as real API changes.
* [ ] **Bring every CI lane under 5 minutes (high priority).** Each Build OS job takes several to tens of minutes
  (macOS slowest), the Kotlin pre-release lane about ten, and the benchmark job over an hour per OS. A lane that slow
  gives no feedback during ordinary work. First read where the time goes (each Build run publishes a Gradle build
  scan), then shard or cut what dominates. Never drop targets, tests or strict dependency verification to win time.
  Also: `build.yml` runs on both `push` (any branch) and `pull_request`, so every same-repo PR, Dependabot's included,
  builds twice on three OSes.
* [ ] **Store event stream** (`FluxoEvent` flow plus an interceptor hook in the store setup). Planned events, each to
  get a test once it exists: bootstrapper cancelled, side-job error (with its key and whether it was a restart),
  side effect undelivered (CONSUME and RECEIVE strategies with a conflated buffer).
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
