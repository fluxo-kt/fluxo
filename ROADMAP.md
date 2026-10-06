
## Roadmap

* [ ] **Restore static analysis (high priority).** Detekt, Spotless and the harness lint setup are off:
  fluxo-kmp-conf enables them only with `setupVerification = true`, which the root build never sets
  (`enableSpotless = true` is inert without it). With the flag on, Spotless reformats a large share of the sources,
  detekt reports several hundred findings (mostly fluxo-core common code). fluxo-kmp-conf 0.16+ applies detekt 2.x
  (`dev.detekt`, alpha), which rejects 1.x keys in `detekt.yml` (`output-reports`, `style>UnusedPrivateMember`), so the
  config must be migrated and the catalog `detekt` 1.x pin re-checked. Plan: migrate the config, one reformat commit,
  fix real findings, baseline the rest with reasons.
* [ ] **Bring every CI lane under 5 minutes** (not a current priority while local builds stay fast). The Build OS jobs,
  the Kotlin pre-release lane and above all the benchmark job are far over it, so they give no feedback during
  ordinary work. First read where the time goes (each Build run publishes a Gradle build scan), then shard or cut what
  dominates. Never drop targets, tests or strict dependency verification to win time.
  Also: `build.yml` runs on both `push` (any branch) and `pull_request`, so every same-repo PR, Dependabot's included,
  builds twice on three OSes. The Dependabot baseline regeneration (`baselines.yml`) runs the full `build` graph so
  test-only artefacts get pinned; a resolve-only task over the test configurations would cut it, once proven to pin
  the same set on a cold cache.
* [ ] **Run every fluxo-core test on every platform.** Many tests carry `@IgnoreJs`/`@IgnoreJvm`/`@IgnoreNative`
  for 2023 CI timeouts (the whole `IntentStrategyTest` is skipped on JS/Wasm, so intent strategies never run there);
  the notes above them cite a 2000 ms timeout that no longer exists and run logs that have expired. Re-enable each,
  reproduce any failure under load with a current stack trace, fix the cause (see `debugIntentLabel` for one such
  cause that was found), then delete the stale notes. Profile the suite too: some unit cases exceed 1 s only when run
  with their class (order or contention, not a fixed wait).
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
