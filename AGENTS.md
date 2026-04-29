# Fluxo — Agent Guide

**Fluxo** /ˈfluksu/ — Kotlin Multiplatform state-management on coroutines + `StateFlow`. Combines strict Redux/MVI correctness with MVVM+ ergonomics (suspend lambda intents, side-jobs, time-travel-ready logging). **Pre-1.0 alpha**; public API is unstable but locked per-commit by `binary-compatibility-validator` (BCV) — but **only on JVM, Android, and JS** (see Gotchas). Targets every KMP platform.

## Meta-rule: if you're surprised, alert + amend

If anything in this project surprises you, contradicts the docs, or would have saved you time to know — **mention it in your reply to the user *and*, in the same turn, append a one-line entry (with a symbol or file:line citation) to the "Gotchas" list below**. If a listed gotcha becomes obsolete, remove it. The compounding cost of un-shared discoveries is the single biggest tax on agent work here.

## Composite build (read this first)

`settings.gradle.kts` does `includeBuild("../fluxo-kmp-conf")`. The Gradle DSL the build relies on (`fkcSetupRaw`, `fkcSetupMultiplatform`, `fkcSetupKotlinApp`, `isRelease()`, `isCI()`) lives in that sibling repo (https://github.com/fluxo-kt/fluxo-kmp-conf). **Without `../fluxo-kmp-conf` checked out alongside, configuration fails with unresolved references.** The plugin is *also* published to Plugin Portal/JitPack — swap `includeBuild(...)` for a versioned alias if you don't want to dogfood. Project default is dogfooding.

`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` is what makes `projects.fluxoCommon` syntax work in module builds.

## Modules

| Path | Role |
|---|---|
| `:fluxo-common` | Internal annotations only: `@InlineOnly`, `@InternalFluxoApi`, `@ExperimentalFluxoApi`. `compileOnly` upstream. |
| `:fluxo-core` | The state container. `Store`/`StoreSE`/`Container`/`ContainerHost`, `FluxoSettings`, `IntentHandler`/`Reducer`, `Bootstrapper`, `SideJob`, `IntentStrategy` (Fifo/Lifo/Parallel/Direct/ChannelLifo), `SideEffectStrategy` (RECEIVE/CONSUME/SHARE/DISABLE), `GuaranteedEffect`, `StoreFactory`/`StoreDecorator`(`Base`), `repeatOnSubscription`, `closeAndWait`. |
| `:fluxo-data` | `FluxoResult<T>` mixed-state value (NotLoaded/Cached/Loading/Empty/Success/Failure, bit-packed flags). Optional. **`compileOnly` deps on coroutines AND fluxo-common — consumers must bring their own.** |
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

1. **`Direct` is the *code* default for `intentStrategy`, but README + KDoc claim `Fifo`.** Both `FluxoSettings.intentStrategy` (initial value) and `ParallelIntentStrategy.Companion.DIRECT` (`= ParallelIntentStrategy(start=UNDISPATCHED)`) confirm. Real contradiction in source docs — don't propagate the wrong claim.
2. **`DEBUG` differs by platform.** Android: variant-aware (`debug=true`, `release=false`, ✅). Pure JVM (`Debug.kt` in `jvmMain`): hardcoded `true` with TODO ❗ — pure-JVM consumers run `IntentStrategyGuardian` even in production unless they explicitly set `debugChecks=false`. JS/Native: `false`.
3. **`fluxo-common/.../InlineOnly.kt` is regenerated on every Kotlin compile** by `inlineOnlySwitcher` (in `fluxo-common/build.gradle.kts`). Don't hand-edit; expect git to show it dirty post-build. Switches between `kotlin.internal.InlineOnly` (release) and a no-op annotation (debug).
4. **`./gradlew apiDump` may produce a dump that differs from the CI-published one** unless run with the env from `.run/apiDump.run.xml` (`RELEASE=true`, `--no-build-cache`, `--no-configuration-cache`, `-Dkotlin.incremental=false`, `--rerun-tasks`). Use `./updateBaselines` (sets all flags) for safety.
5. **Public API is locked only on JVM/Android/JS** (`*/api/{android,jvm}/*.api` + `*/api/js/*.d.ts` via sibling `fluxo-bcv-js` plugin). **Apple/Linux/MinGW targets are NOT BCV-locked** — drift on those targets won't fail CI.
6. **`FluxoSettings.DEFAULT` is a `@NotThreadSafe` global mutable singleton.** Configure once at app start; mutating it later affects every later-built store.
7. **Setter cascades** in `FluxoSettings`: `debugChecks=true` ⇒ `closeOnExceptions=true`; setting `exceptionHandler` non-null ⇒ disables `closeOnExceptions`; `sideEffectStrategy=SHARE(...)` ⇒ flips a `BUFFERED` (-1) buffer to `0`. `copy()` runs assignments in reverse to neutralise these — keep that order if extending.
8. **`SideEffectStrategy.CONSUME` is single-subscriber** (`consumeAsFlow`); resubscribing throws. Use `RECEIVE` (default channel multiplex) or `SHARE` (`MutableSharedFlow` broadcast) for multiple collectors.
9. **Per-module `dependencies/*.txt`** baselines (`dependencyGuard` plugin) — any dep change must regenerate via `./gradlew dependencyGuardBaseline` or `./updateBaselines`.
10. **Benchmarks default to a stale snapshot** of fluxo-core (`fluxoSnapshot` in `libs.versions.toml`). Set `CI=true` or `RELEASE=true` to dogfood the local build instead. Bump `fluxoSnapshot` after publishing a new snapshot.
11. **Only `Store.stateFlow`, `Store.state`, `StoreScope.launch`, `StoreScope.async` are `@JvmSynthetic`-hidden** from Java/iOS. Inline `accept`/`orbit` rely on `@InlineOnly`. Kover excludes `*Synthetic*`/`*Inline*`/`*Deprecated*` only in release mode → debug vs release coverage numbers aren't the same scale.
12. **`compareAndSet` on stored state closes the previous state** if `expect != update`, *and* the new state if `expect !== update` but `expect == update` — the experimental "Closeable as state" feature. Returning a new equal instance still triggers `closeSafely()` on it.
13. **`emit` vs `send`**: `emit` is the suspend `FlowCollector` form (no `Job`); `send` returns a `Job` for non-suspend callers. **Joining the returned `Job` is dangerous** (deadlock-prone) and explicitly discouraged.
14. **Module list duplicated** in `settings.gradle.kts` and `.github/workflows/build.yml` — keep both in sync (settings file warns).
15. **K/Native uses old experimental memory model + native compiler daemon disabled** (`gradle.properties`). Slower native builds; brittle on K/N upgrades.
16. **`kotlinx-atomicfu` plugin rewrites bytecode at compile time** → `atomic()` works without a runtime artefact. Buildscript classpath includes the plugin.
17. **KT-58512** breaks IDE "Go to declaration" / "Quick Documentation" on `container { }` and similar inline builders. Known issue, not actionable here.

## Common commands

```
./gradlew check                                  # full verify (lint, detekt, tests, kover, apiCheck)
./gradlew :fluxo-core:jvmTest                    # fast inner loop
./updateBaselines                                # regenerate ALL baselines (lint, detekt, dep guard, api, yarn lock) with correct env
./gradlew dependencyGuardBaseline                # regenerate only dep snapshots
./gradlew :benchmarks:jmh:jmh                    # run JMH suite (filter via `IncrementIntent.*` regex)
./gradlew -Dsplit_targets ...                    # split KMP targets across CI shards (Windows uses this)
RELEASE=true ./gradlew ...                       # release mode (IndyLambdas, real InlineOnly)
CI=true ./gradlew :benchmarks:jmh:jmh            # benchmark against the LOCAL fluxo-core, not the snapshot
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
- **Conventional Commits required.** Allowed types: `feat|fix|test|build|ci|docs|perf|refactor|style|chore|i18n|deps|misc|revert`. PR titles same format. Imperative present tense.
- **Don't introduce dependencies.** Fluxo is "small and light" by stated policy. Public API changes need explicit reasoning in the PR; BCV diff must be committed.
- Keep git history flat; no merge commits except hotfix branches.

## Testing

- `commonTest` for KMP-wide tests; platform-specific tests in their own source sets. Use `runUnitTest`/`runTest` + `CoroutineScopeAwareTest` + `TestFlowObserver` from `kt.fluxo.test`.
- Coverage thresholds enforced via Kover; failing them fails `check`. See `koverReport` block in root `build.gradle.kts` for current values.
- No instrumented Android tests; integrations (Compose, ViewModel, Essenty, Arrow, LiveData) are roadmap, not shipped.

## Where deeper docs live

- Usage examples + benchmarks: `README.md`. Roadmap: `ROADMAP.md`. Releasing: `RELEASING.md` (currently a stub).
- Per-module READMEs are minimal/single-line.
- **Deepest semantics live in KDoc inside source.** Start with `FluxoSettings`, `FluxoStore.<init>` and `onStart`/`onIntent`, `IntentStrategy`, `SideEffectStrategy`, `GuaranteedEffect`, `StoreScope`, `IntentStrategyGuardian`.

## Vibe & philosophy

- *"Combine strict Redux/MVI correctness with MVVM+ flexibility, readability, and maintainability."* The hypothesis every API decision serves (per README).
- One-liner creation, automatic type inference, no boilerplate. Ceremony = wrong design.
- Performance is a feature — defending the JMH score matters; benchmark before/after micro-changes.
- **Multiplatform-first, Android-second.** Don't add JVM-only or Android-only API to common code.
- Don't break public API lightly. BCV enforces it on JVM/Android/JS (Apple/Linux not enforced — extra care needed there).
- Side effects are an antipattern; supported but discouraged. Prefer state.
