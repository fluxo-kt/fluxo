# Fluxo — Agent Guide

**Fluxo** /ˈfluksu/ — Kotlin Multiplatform state-management on coroutines + `StateFlow`. Combines strict Redux/MVI correctness with MVVM+ ergonomics (suspend lambda intents, side-jobs, time-travel-ready logging). **Pre-1.0 alpha**; public API is unstable but locked per-commit by `binary-compatibility-validator` (BCV): JVM bytecode, Android-main bytecode, JS/Wasm declarations, and KLib ABI. Targets every KMP platform.

## Meta-rule: if you're surprised, alert + amend

If anything in this project surprises you, contradicts the docs, or would have saved you time to know — **mention it in your reply to the user *and*, in the same turn, append a one-line entry (with a symbol or file:line citation) to the "Gotchas" list below**. If a listed gotcha becomes obsolete, remove it. The compounding cost of un-shared discoveries is the single biggest tax on agent work here.

## Composite build (read this first)

`settings.gradle.kts` **conditionally** `includeBuild`s the sibling harness — guard computed *inside* `pluginManagement {}` (it evaluates before the script body, so a top-level val is out of scope there). The Gradle DSL the build relies on (`fkcSetupRaw`, `fkcSetupMultiplatform`, `fkcSetupKotlinApp`, `isRelease()`, `isCI()`) lives in that sibling repo (https://github.com/fluxo-kt/fluxo-kmp-conf). It dogfoods the local sibling **only** when it exists **and** not on CI; otherwise it resolves the **published** `io.github.fluxo-kt.fluxo-kmp-conf` plugin (catalog-pinned), so a fresh single-repo checkout and every CI run build exactly what external consumers get. Force either side with `-Pfluxo.dogfood=true|false`. Local default is dogfooding.

`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` is what makes `projects.fluxoCommon` syntax work in module builds.

## Modules

| Path | Role |
|---|---|
| `:fluxo-common` | Internal annotations only: `@InlineOnly`, `@InternalFluxoApi`, `@ExperimentalFluxoApi`. `compileOnly` upstream. |
| `:fluxo-core` | The state container. `Store`/`StoreSE`/`Container`/`ContainerHost`, `FluxoSettings`, `IntentHandler`/`Reducer`, `Bootstrapper`, `SideJob`, `IntentStrategy` (Fifo/Lifo/Parallel/Direct/ChannelLifo), `SideEffectStrategy` (RECEIVE/CONSUME/SHARE/DISABLE), `GuaranteedEffect`, `StoreFactory`/`StoreDecorator`(`Base`), `repeatOnSubscription`, `closeAndWait`. |
| `:fluxo-data` | `FluxoResult<T>` mixed-state value (NotLoaded/Cached/Loading/Empty/Success/Failure, bit-packed flags). Optional; exposes `fluxo-common` and coroutines as API dependencies. |
| `:benchmarks:jmh` | JVM-only JMH harness comparing Fluxo vs ~14 libs (Ballast, OrbitMVI, MVIKotlin, FlowMVI, Redux, MVICore, …). Single-platform via `fkcSetupKotlinApp`. |

KMP source-set intermediates `commonJvmMain`/`commonJvmTest`, `nonJvmMain`/`nonJvmTest`, `appleTest` are created by `fluxo-kmp-conf`, not stock Kotlin. Test helpers live in package `kt.fluxo.test`; tests proper in `kt.fluxo.tests` — keep the convention.

## Public API entry points

- `container(initial) { … }` → `ContainerS<State>` / `Container<S, SE>` — MVVM+ suspend lambda intents.
- `store(initial, reducer = …)` → strict pure-reducer MVI `Store<I, S>`.
- `store(initial, handler = …)` → discrete intents + MVVM+ `IntentHandler` DSL.
- `CoroutineScope.container/store(...)` extensions inherit caller `coroutineContext`.
- `Container<S, SE>` is a **typealias** for `StoreSE<FluxoIntent<S, SE>, S, SE>` (matters for Java consumers and migration helpers).
- `ContainerS<State>` (no-SE factory) **forcibly sets `sideEffectStrategy=DISABLE`** — these stores cannot post side effects even if you flip the setting later.
- `Store` IS `StateFlow<State>` + `FlowCollector<Intent>` + `CoroutineScope` + `Closeable`. `closeAndWait()` (experimental) drains.
- DSL inside intent/sideJob/bootstrapper (`StoreScope`): `updateState { it + 1 }`, `value = …`, `compareAndSet`, `postSideEffect`, `sideJob(key) { wasRestarted -> … }`, `repeatOnSubscription { … }`, `noOp()`. Guardian rejects `sideJob` blocks that aren't last in the handler.
- All factories accept `settings: FluxoSettings? = null`, `factory: StoreFactory? = null`, `setup: FluxoSettings.() -> Unit = {}`. `setup` runs on a defensive `copy()` of `settings` — original is never mutated.
- `Store` subtypes (`StoreSE`, `StoreScope`, `StoreDecorator`, `StoreFactory`) are `@SubclassOptInRequired(ExperimentalFluxoApi::class)`. For decorators extend `StoreDecoratorBase` (manual delegation, by design — keeps the public surface small).

## Gotchas

Each cites a symbol or file so you can verify in one read.

1. **`Direct` is the default `intentStrategy`.** `FluxoSettings.intentStrategy` initial value and `ParallelIntentStrategy.Companion.DIRECT` (`= ParallelIntentStrategy(start=UNDISPATCHED)`) confirm. Do not reintroduce the old FIFO-default docs.
2. **`DEBUG` differs by platform.** Android/JS/Native: `false` by default. Pure JVM (`Debug.kt` in `jvmMain`): hardcoded `true` with TODO ❗ — pure-JVM consumers run `IntentStrategyGuardian` even in production unless they explicitly set `debugChecks=false`.
3. **`InlineOnly` compatibility wrapper is no-op; real inline-only uses `kotlin.internal.InlineOnly` directly.** Kotlin 2.3 rejects the old public actual typealias to hidden `kotlin.internal.InlineOnly`, so don't reintroduce it in `fluxo-common/build.gradle.kts`.
4. **`./gradlew apiDump` may produce a dump that differs from the CI-published one** unless run with the env from `.run/apiDump.run.xml` (`RELEASE=true`, `--no-build-cache`, `--no-configuration-cache`, `-Dkotlin.incremental=false`, `--rerun-tasks`). Use `./updateBaselines` (sets all flags) for safety.
5. **API validation is intentionally multi-lane.** `apiCheck` must run `jvmApiCheck`, custom `androidApiCheck` and `klibApiCheck`; `apiDump` writes `<module>/api/{jvm,android}` and `<module>/api/*.klib.api` baselines. If any of these lanes is skipped unexpectedly, treat it as a build-logic regression. The TypeScript-declaration lane (`tsApiChecks`, `api/js/*.d.ts`) is wired but inactive, so its green is vacuous and `api/js` is stale: the harness already calls KGP's non-idempotent `generateTypeScriptDefinitions()` (fluxo-kmp-conf `KotlinJsUtils.kt`), fluxo-bcv-js 1.1.0 calls it again inside the same guard that collects the compilations, and the duplicate-task failure leaves it with zero `.d.ts` tasks. It activates with the first fluxo-bcv-js release containing commit be5369c; then bump the plugin and review the first regenerated `.d.ts` dumps as real API changes.
6. **`FluxoSettings.DEFAULT` is a `@NotThreadSafe` global mutable singleton.** Configure once at app start; mutating it later affects every later-built store.
7. **Setter cascades** in `FluxoSettings`: `debugChecks=true` ⇒ `closeOnExceptions=true`; setting `exceptionHandler` non-null ⇒ disables `closeOnExceptions`; `sideEffectStrategy=SHARE(...)` ⇒ flips a `BUFFERED` (-1) buffer to `0`. `copy()` runs assignments in reverse to neutralise these — keep that order if extending.
8. **`SideEffectStrategy.CONSUME` is single-subscriber** (`consumeAsFlow`); resubscribing throws. Use `RECEIVE` (default channel multiplex) or `SHARE` (`MutableSharedFlow` broadcast) for multiple collectors.
9. **Per-module `dependencies/*.txt`** baselines (`dependencyGuard` plugin) — any dep change must regenerate via `./gradlew dependencyGuardBaseline` or `./updateBaselines`.
10. **Benchmarks dogfood local `:fluxo-core` directly.** Do not reintroduce a published snapshot default; stale/inaccessible snapshot metadata breaks root `check` before benchmarks run (`benchmarks/jmh/build.gradle.kts`). They need fluxo-core's JVM target, so `settings.gradle.kts` leaves the module out of narrowed-target builds (`-Dsplit_targets`, `KMP_TARGETS`); CI Windows runs split and has no JVM target.
11. **Only `Store.stateFlow`, `Store.state`, `StoreScope.launch`, `StoreScope.async` are `@JvmSynthetic`-hidden** from Java/iOS. Inline `accept`/`orbit` rely on `@InlineOnly`. Kover excludes `*Synthetic*`/`*Inline*`/`*Deprecated*` only in release mode → debug vs release coverage numbers aren't the same scale.
12. **`compareAndSet` on stored state closes the previous state** if `expect != update`, *and* the new state if `expect !== update` but `expect == update` — the experimental "Closeable as state" feature. Returning a new equal instance still triggers `closeSafely()` on it.
13. **`emit` vs `send`**: `emit` is the suspend `FlowCollector` form (no `Job`); `send` returns a `Job` for non-suspend callers. **Joining the returned `Job` is dangerous** (deadlock-prone) and explicitly discouraged.
14. **Module list lives only in `settings.gradle.kts`.** CI (`build.yml`) runs *root* Gradle tasks (`build`/`check`/`publishToMavenCentral`) that auto-discover modules — it enumerates none, so adding a module needs no `build.yml` edit.
15. **K/Native compiler daemon is disabled** (`gradle.properties`). Native builds can be slower; re-check this on K/N upgrades.
16. **`org.jetbrains.kotlinx.atomicfu` plugin rewrites bytecode at compile time** → `atomic()` works without a runtime artefact. Keep it on the plugin DSL alias in modules that use atomicfu.
17. **KT-58512** breaks IDE "Go to declaration" / "Quick Documentation" on `container { }` and similar inline builders. Known issue, not actionable here.
18. **`FluxoSettings.coroutineContext` defaults to `Dispatchers.Default`, not `Main`.** UI/view-bound stores (Android, Compose Desktop, etc.) must explicitly set `coroutineContext = Dispatchers.Main` or pass a Main-bound `scope`. Wrong default for view consumers — symptom is state updates landing off the main thread.
19. **Reducer-based stores without a `bootstrapper` have side jobs disabled.** `FluxoStore.<init>` skips creating `sideJobsMap` when `intentHandler is ReducerHandler` and `bootstrapper == null` (perf optimization). `sideJob` / `repeatOnSubscription` then throw `"Side jobs are disabled for the current store: …"` at runtime. Switch to an `IntentHandler` or set a bootstrapper to enable them.
20. **Apple simulator SDK presence is not enough for simulator tests.** `xcrun --sdk appletvsimulator --show-sdk-path` can pass while `simctl list runtimes` has no tvOS/watchOS runtime; `fluxo-kmp-conf` skips affected test compile/link/run tasks at task time, not target construction time.
21. **AGP 9 rejects old global Android flags** in `gradle.properties` (`android.defaults.buildfeatures.*`, `android.experimental.*` lint/resource flags). Configure required Android features in module DSL, not root properties.
22. **AGP 9 KMP Android must be explicitly wired under `commonJvmMain`.** `KotlinHierarchyTemplate.fluxoKmpConf` alone left `androidMain` as a sibling; sibling `fluxo-kmp-conf` now bridges `androidMain -> commonJvmMain` in `SetupKotlin.kt`.
23. **Kotlin/JS default-argument bridges can recurse for overridable `StoreScope.sideJob`.** Keep `StoreScope.sideJob` as explicit overloads plus one full `@JsName("sideJob")` implementation; restoring default parameters reintroduces `RangeError: Maximum call stack size exceeded` in `StoreDecoratorTest.interface_check`.
24. **Diagnostic Kotlin target/source-set print tasks are not configuration-cache safe.** `:fluxo-core:printKotlinTargetsInfo` succeeds but discards the configuration cache because the task captures `Project`; do not use it as a CC gate until the helper task is fixed.
25. **Merged test report results are execution-time shared state.** In `fluxo-kmp-conf`, keep `TestReportService` results in service-owned synchronized storage; Gradle `ListProperty.add` from parallel `TestListener.afterTest` callbacks corrupts provider state.
26. **The external JMH Gradle plugin is not configuration-cache compatible here.** Run JMH with `--no-configuration-cache` and keep `benchmarks/jmh` JMH tasks marked `notCompatibleWithConfigurationCache`; otherwise Gradle 9 reports `:benchmarks:jmh:jmhJar` Project serialization problems after benchmark execution.
27. **Publishing needs four independent layers — drop any one and it silently breaks/mis-targets** (see `RELEASING.md`): (a) `enablePublication = true` in root `build.gradle.kts` (harness gate); (b) the vanniktech plugin `alias`-applied in *each* library module **and** `apply false` at root — Gradle plugin-classloader isolation means the harness configures but cannot apply it; (c) the `version` catalog key (a wrong key falls through to `unspecified`); (d) per-module `projectName` (else all modules collide on the root `artifactId`). JitPack (`jitpack.yml`, any commit on request) sets `JITPACK=true`; root `build.gradle.kts` then publishes under JitPack's `GROUP.ARTIFACT` and `VERSION` (module metadata links variants by group and version) without Dokka, so keep any `group`/`version` change inside that switch. JitPack's sandbox answers `statx` with EPERM, which breaks JDK 25, so its daemon runs on JDK 21.
28. **`kotlinCoreLibraries` (published stdlib) must not exceed `kotlin` (compiler); it deliberately sits below it.** JVM/Android artefacts declare stdlib `kotlinCoreLibraries`, kept on the consumer-floor line (`kotlinLangVersion`), so Kotlin 2.3 consumers are not forced onto a newer stdlib; JS/Wasm resolve the compiler's stdlib and kotlin-test because their klib ABI must match the compiler; fluxo-kmp-conf 0.16+ does that resolution, so do not re-add a root rule for it. The harness rejects a stdlib newer than the compiler (`kotlinStdlibSkewError`); never suppress with `-Xskip-runtime-version-check`.
29. **Regenerate `verification-metadata.xml` only with `./updateBaselines` (or `--deps-only`), on macOS.** A hand-run `--write-verification-metadata` silently misses parent POMs/BOMs, test-only and other-host artefacts, and CI then fails; the comment above its Gradle calls says why each task is there. Linux and Windows skip the Apple test and link tasks, so their artefacts would go unpinned. The dogfood predicate in `settings.gradle.kts` disables composite mode under `--write-verification-metadata` and `dependencyGuardBaseline`, so the published harness graph is what gets pinned. That file ALSO governs the included sibling harness build (Gradle ignores an included build's own file), so dogfood builds always meet unpinned harness dependencies. Hence local builds verify leniently (`gradle.properties` `org.gradle.dependency.verification=lenient`: mismatches are printed, not fatal) and CI verifies strictly: every Gradle-running workflow puts `-Dorg.gradle.dependency.verification=strict` in `GRADLE_OPTS`, and `settings.gradle.kts` fails a `CI=true` build without it (exempt: metadata writes, and `-Pfluxo.kotlin`, which only the lenient Kotlin pre-release lane sets). A new workflow that runs Gradle MUST set it. A command-line `-D` beats `GRADLE_OPTS` and the last `-D` wins, so CHECK any action that runs Gradle itself: `gradle/actions/dependency-submission` passes `=off`, and `deps-submission.yml` appends a later `=strict`. Reproduce CI locally: `-PCI=true --dependency-verification strict` (the harness reads the property like the env var; the guard reads only the env var). The mode cannot be switched from a settings script — Gradle fixes it before settings run.
30. **Platform-keyed target/source-set lookups MUST be lazy + absence-tolerant.** Harness CI variants (e.g. `split_targets` on Windows runners) filter the KMP target list, so eager `targets.named("android")` or `sourceSets.named("androidMain")` throws `UnknownDomainObjectException` at configuration time. Always use `targets.matching { it.name == "<name>" }.configureEach { … }` (and the equivalent for `sourceSets`) — lazy, no-op when absent, configuration-cache safe. For multi-name loops, hoist the set and match in one call (`sourceSets.matching { it.name in nameSet }.configureEach { … }`). The eager form is invisible on local composite (every target is registered) and only surfaces under CI sharding.
31. **YAML `permissions: pull-requests: write` does NOT override the *org-level* `Allow GitHub Actions to create and approve pull requests` toggle** (default off since 2022; propagates to every repo in the org and supersedes repo-level overrides — `PUT /repos/.../actions/permissions/workflow` returns `409 Conflict: The organization does not allow GitHub Actions to create or approve pull requests`). Only an **org admin** at `https://github.com/organizations/fluxo-kt/settings/actions` can flip it. While off, `gh pr create` from `github-actions[bot]` exits 1 with GraphQL `GitHub Actions is not permitted to create or approve pull requests (createPullRequest)` — branch+commit are pushed successfully *before* the failure. Pattern: every workflow that opens PRs must fail-soft on that exact stderr substring and emit a `::notice::` with the compare URL (see `.github/workflows/jmh-baseline-bootstrap.yml`'s `gh pr create` block). Repo maintainers without org-admin cannot fix this any other way.
32. **Detekt, Spotless and the harness lint setup do not run.** fluxo-kmp-conf enables them only with `setupVerification = true` (default `false`), which the root `fkcSetupRaw` never sets, so `enableSpotless = true` and the `detekt-baseline.xml` files are inert. Restoring them needs a detekt 2.x config migration (the harness applies detekt 2), one large reformat and a review of the findings. Do not cite detekt or Spotless as passing evidence.
33. **The Gradle daemon runs a newer JDK than the code targets; a JVM toolchain keeps compilation and tests on the target JDK.** `gradle/gradle-daemon-jvm.properties` asks for JDK 25 (KSP processors built for Java 21+ load in the daemon); root `fkcSetupRaw { setupJvmToolchain = true }` makes the harness call `jvmToolchain(javaLangTarget)`, so libraries compile against and test on JDK 17 (benchmarks target 25). Without it kotlinc and `jvmTest` silently follow the daemon JDK. Toolchain auto-download is off: install both JDKs (SDKMAN locally; `setup-java` lists 17 and 25 on CI). The harness logs "JVM toolchain setup is enabled! … rarely beneficial" — expected here. Kotlin 2.4 also warns `KLIB loader: same unique_name=kotlin-stdlib-common` on native metadata compilation; non-fatal, unrelated to the split floor.
34. **Never interpolate an intent or other lambda into a string in store code; use `debugIntentLabel`** (`Debug.common.kt`). On JVM with kotlin-reflect on the classpath (fluxo-kmp-conf adds it to every `commonTest`; many apps carry it), a lambda's `toString()` renders its signature through full reflection: 0.5–2 s on first use, which made `b_intent`/`b_side_job` time out on loaded CI. Discrete intents and `FluxoIntentDebug` keep their own `toString()`.

## Common commands

```
./gradlew check                                  # full verify (tests, kover, ABI, dependency guard; NOT lint/detekt/Spotless — gotcha #32)
./gradlew :fluxo-core:jvmTest                    # fast inner loop
./updateBaselines                                # regenerate ALL baselines (yarn locks, dep guard, verification metadata, API dumps) with correct env
./updateBaselines --deps-only                    # only dependency-derived baselines (what the Dependabot workflow runs)
./gradlew dependencyGuardBaseline                # regenerate only dep snapshots
./gradlew :benchmarks:jmh:jmh --no-configuration-cache # run JMH suite (filter via `IncrementIntent.*` regex; dogfoods local `:fluxo-core`)
./gradlew -Dsplit_targets ...                    # split KMP targets across CI shards (Windows uses this)
RELEASE=true ./gradlew ...                       # release mode (IndyLambdas, stricter baselines)
```

For `apiDump`, see Gotcha #4.

## Architecture notes (high-leverage, not greppable)

- `FluxoStore` is the single concrete store; everything else (`DebugStoreDecorator`, `GuardedStoreDecorator`, custom decorators) wraps via `StoreDecorator`. The store IS a `CoroutineExceptionHandler` (extends `AbstractCoroutineContextElement`) for ergonomic context plumbing.
- **Leak-free transfer pattern** (Kotlin/kotlinx.coroutines#1936) appears twice: side effects in `FluxoStore.<init>` and intents in `ChannelBasedIntentStrategy.<init>`. Both use `Channel.onUndeliveredElement` + Mutex-guarded recursion-safe resend, then `closeSafely()` if not redeliverable.
- **State rollback on intent cancellation** when `parallelProcessing=false` (Fifo/Lifo/ChannelLifo). Parallel/Direct skip rollback. Implemented in `IntentStrategy.executeIntent`.
- `subscriptionCount` sums state subscribers + side-effect subscribers via a custom `CombinedFlowCounter` (`Util.kt`). Backs `repeatOnSubscription`.
- `kotlin.internal.LowPriorityInOverloadResolution` disambiguates `container(...)` (no-SE) vs `containerWithSideEffects(...)`. `@JvmName`/`@JsName`/`@ObjCName` rename per platform — public-API names are co-designed for JS/ObjC consumers.
- Migration ergonomics: `accept`, `orbit`, `launch`, `async`, `state`, `stateFlow`, `reduce` are deprecated shadows so Orbit/MviKotlin code compiles with quick-fixes (`Migration.kt`, `StoreScope`). Don't add more lightly.
- `GuaranteedEffect<T>` — exactly-once delivery via atomic `hasBeenHandled` + `handleOrResend { … }` + a re-send hook injected by the store on first publication.
- `IntentStrategyGuardian` enforces: state access only OK before `sideJob`; **`sideJob` blocks must be last** statements of handler/bootstrapper; handlers must do "something" or call `noOp()`. Active only when `debugChecks=true` (so always-on for pure-JVM by default — see Gotcha #2).

## Code style & contribution

- `explicitApi()` is on for all libs; every `public` is intentional.
- `allWarningsAsErrors = true`; new warnings break the build.
- **Conventional Commits required.** Allowed types: `feat|fix|test|build|ci|docs|perf|refactor|style|chore|i18n|deps|revert` (canonical list lives in `.commitlintrc.yml`). PR titles same format. Imperative present tense.
- **Don't introduce dependencies.** Fluxo is "small and light" by stated policy. Public API changes need explicit reasoning in the PR; BCV diff must be committed.
- **Scripts: inline-declare deps; no lockfile.** `.main.kts` uses `@file:DependsOn("group:artifact:ver")`. A new Bun TS script must use Bun's `import x from "npm:foo@1.2.3"` syntax — DO NOT add `bun.lock`/`package.json` unless ≥3 scripts share deps. Single-consumer lockfiles are pure maintenance burden.
- Keep git history flat; no merge commits except hotfix branches.
- Work lands on `dev`, the GitHub default branch (Dependabot, dependency submission and default-branch CI gates follow it); `main` is the trailing release line, and snapshots publish only from `main`.

## Testing

- `commonTest` for KMP-wide tests; platform-specific tests in their own source sets. Use `runUnitTest`/`runTest` + `CoroutineScopeAwareTest` + `TestFlowObserver` from `kt.fluxo.test`.
- Coverage thresholds enforced via Kover; failing them fails `check`. See `koverReport` block in root `build.gradle.kts` for current values.
- No instrumented Android tests; integrations (Compose, ViewModel, Essenty, Arrow, LiveData) are roadmap, not shipped.

## Where deeper docs live

- Usage examples + benchmarks: `README.md`. Roadmap: `ROADMAP.md`. Releasing: `RELEASING.md` (tag-push → vanniktech → Central Portal; secrets, snapshot/release split, the 4-layer publication wiring).
- Per-module READMEs are minimal/single-line.
- **Deepest semantics live in KDoc inside source.** Start with `FluxoSettings`, `FluxoStore.<init>` and `onStart`/`onIntent`, `IntentStrategy`, `SideEffectStrategy`, `GuaranteedEffect`, `StoreScope`, `IntentStrategyGuardian`.

## Vibe & philosophy

- *"Combine strict Redux/MVI correctness with MVVM+ flexibility, readability, and maintainability."* The hypothesis every API decision serves (per README).
- One-liner creation, automatic type inference, no boilerplate. Ceremony = wrong design.
- Performance is a feature — defending the JMH score matters; benchmark before/after micro-changes.
- **Multiplatform-first, Android-second.** Don't add JVM-only or Android-only API to common code.
- Don't break public API lightly. BCV enforces JVM, Android-main, JS/Wasm declarations, and KLib ABI.
- Side effects are an antipattern; supported but discouraged. Prefer state.
