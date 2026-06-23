# Changelog

## Unreleased

### Breaking changes (consumer-facing)

- **JDK 17+ required at runtime** (was Java 8). Class-file major version 61.
  Bytecode targets Java 17; consumers on older JREs cannot load the artefacts.
- **Kotlin 2.3+ stdlib floor at runtime** (was 1.8). `kotlinCoreLibraries` is
  pinned at 2.3.21 for the build, matching the compiler. Older stdlib produces
  a version-mismatch warning that `allWarningsAsErrors` turns fatal.
- **Android `minSdk = 21`** (was 9). Devices below API 21 are no longer
  supported.
- **Coroutines context-key migration.** `coroutineContext[CoroutineDispatcher]`
  is deprecated by `kotlinx.coroutines 1.11+`; use
  `coroutineContext[ContinuationInterceptor]` instead. Same JVM key under the
  hood; the deprecation is to align with the multiplatform contract.

### Internal (no consumer impact)

- Build toolchain: Kotlin 2.3.21 / Gradle 9.6.0 / AGP 9.2.1 KMP plugin /
  fluxo-kmp-conf 0.15.0 alias (composite still primary per invariant I1) /
  Develocity 4.4.3 / Compose-MP plugin alias 1.9.3 (Plugin Portal ceiling).
- Build: standalone `kotlinx-atomicfu` plugin retained on the modern plugin id
  (the KGP-embedded path is inadequate for fluxo's atomic bytecode rewrite —
  AGENTS.md gotcha #16).
- Test: coroutines 1.11.0; benchmark harness API refresh.
- Benchmarks (comparison frameworks, JDK-17-bounded): ballast 4.1→5.1.0,
  flowredux 1.2.1→1.2.2, mobiusKt 1.2.1→1.4.0, mvikotlin 4.0→4.4.0,
  motorroCommonStateMachine 3.2.0→3.3.0 (4.x line raises bytecode to JDK 21,
  unsupported on fluxo JDK 17), orbit 7.1.0→11.0.0 (4-major; DSL flattened
  from `syntax.simple.*` to `syntax.Syntax`), respawnFlowMVI alpha11→3.2.1
  stable (`useState` renamed to `updateStateImmediate` in 3.x), visualfsm
  2.0.0→3.0.0 (4.x requires KSP code generation, out-of-scope for the
  benchmark module). MVICore 2.0.0 deferred — upstream JitPack build is
  broken on a missing `debugdrawer-base:0.9.0` transitive.
- Verification: new `checkForbiddenFlags` drift gate rejects deprecated /
  removed-upstream / no-op `gradle.properties` keys at `check` time; each
  forbidden key carries an inline rationale string.
- JMH: `compareAgainstBaseline()` helper in `benchmark-summary.main.kts`
  applies the dual gate `|Δ|/σ>2 AND |Δ|/base>5%` when
  `JMH_BASELINE_CHECK=1`; baseline JSON anchoring lands separately via CI.
- Supply-chain (release-only): `dev.sigstore.sign` 2.2.0 auto-signs every
  `MavenPublication` with a Sigstore bundle; `org.cyclonedx.bom` 3.2.4 emits a
  per-module CycloneDX SBOM (full + direct scopes, JSON + XML). The release
  workflow attaches all of them, plus the Sigstore bundles, as GitHub Release
  assets. Verification commands are in `RELEASING.md`.
- Supply-chain (build-side): Gradle dependency-verification metadata at
  `gradle/verification-metadata.xml` enforces SHA-256 pinning of every resolved
  artefact in the graph. CDN/repo poisoning of any direct or transitive
  dependency is rejected at resolve time. PGP signature pinning is left
  advisory for now (`verify-signatures=false`); SHA-256 alone defends against
  artefact substitution. New workflow `verify-metadata.yml` auto-regenerates
  the file on Dependabot PRs and force-with-lease-pushes the update back.

### Removed

- Catalog dead entries: `arrow`, `arrow-bom`, `arrow-core`, `kotlinx-lincheck`,
  `plugin-intellij`, `test-robolectric` — zero live source consumers.
- Catalog `js-karma`, `js-uaParserJs`: harness `setFromCatalog` is a
  conditional override — absent key falls back to KGP-bundled defaults.
  Karma is replaced by Mocha at the harness level
  (`KotlinJsUtils.kt:142 useMocha`).
- `gradle/plugins/` legacy harness tree — fully superseded by the
  `fluxo-kmp-conf` composite (`fkcSetup*` DSL).
- `Dockerfile` + `.run/Dockerfile.run.xml` — orphan CircleCI deploy artefacts;
  CI has been GitHub Actions since the harness migration.
- OSSRH snapshot infrastructure — shutdown 2025-06-30; replaced by Sonatype
  Central Portal (`publishToMavenCentral` / `publishAndReleaseToMavenCentral`).
