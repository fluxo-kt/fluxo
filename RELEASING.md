# Release instructions

Fluxo's libraries (`fluxo-core`, `fluxo-common`, `fluxo-data`) publish to **Maven Central via
the [Central Portal](https://central.sonatype.com/)**. Publishing is 100% [vanniktech
maven-publish](https://github.com/vanniktech/gradle-maven-publish-plugin) — the legacy OSSRH /
`oss.sonatype.org` path is retired (shut down 2025-06-30) and must not be reintroduced.

The single source of truth for the published version is the `version` key in
`gradle/libs.versions.toml`. A `-SNAPSHOT` suffix marks a snapshot; anything else is a release.

## Prerequisites

- **The published `fluxo-kmp-conf` harness must already support this build stack.** CI builds a
  *fresh single-repo checkout* against the **published** plugin (not the local sibling — see the
  hermeticity note in `AGENTS.md`). If a release needs a harness change, that harness version must
  be released and the catalog pin bumped to it **first**, otherwise CI/release cannot reproduce the
  build. Verify with `curl` against `maven-metadata.xml` (see Verification).
- **Maintainer-provisioned repository secrets** (GitHub → Settings → Secrets and variables →
  Actions). These are outside contributor/agent access:

  | Secret | Purpose |
  |---|---|
  | `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` | Central Portal **user token** (generate at central.sonatype.com → Account). |
  | `SIGNING_KEY` | ASCII-armored PGP **private** key block (the verbatim `-----BEGIN PGP PRIVATE KEY BLOCK-----…` text, real newlines). |
  | `SIGNING_PASSWORD` | Passphrase for `SIGNING_KEY`. |
  | `SIGNING_KEY_ID` | *(optional)* short key id, only if the key has subkeys. |
  | `CODECOV_TOKEN` | Coverage upload (build.yml), unrelated to publishing. |

  Replace any legacy `OSSRH_*` secrets — they are dead infra and unused by the new workflows. To
  produce a CI-safe `SIGNING_KEY` value from a local keyring, use the harness's
  `export-release-signing-key.sh` (normalizes the armored block).

## Automatic

CI publishing is wired across two workflows; both feed vanniktech's Central Portal credentials via
`ORG_GRADLE_PROJECT_*` environment variables (the plugin reads them as Gradle project properties).

### Snapshots — on every push to `main`

`.github/workflows/build.yml` runs `publishToMavenCentral` (the **unsigned** snapshot task — the
harness marks PGP signing required only for non-snapshot versions). Snapshots land in the Central
Portal snapshots repository: `https://central.sonatype.com/repository/maven-snapshots/`.

Consumers opt in with that repository plus the `-SNAPSHOT` coordinate (see `README.md`).

### Releases — on a `vX.Y.Z` tag push

1. Set a non-snapshot `version` in `gradle/libs.versions.toml` (`version = "X.Y.Z"`).
2. Commit (`build(release): X.Y.Z`).
3. Push a matching tag: `git tag vX.Y.Z && git push origin vX.Y.Z`.

`.github/workflows/release.yml` (trigger `tags: v*`) then:

- verifies all four release secrets are present and that `SIGNING_KEY` is a real PGP block;
- **verifies the tag matches the resolved project version** and is not a snapshot (fail-fast — a
  `v*` tag can never publish a mismatched or unsigned-snapshot artifact);
- runs the full release build (`apiCheck androidApiCheck build assemble check`);
- generates a per-module CycloneDX SBOM (`cyclonedxBom`) before publish;
- runs `publishAndReleaseToMavenCentral` (PGP-signed for Central + Sigstore-signed via the
  `dev.sigstore.sign` plugin, which auto-attaches `.sigstore.json` bundles to every
  `MavenPublication`; both auto-promote the Central Portal deployment);
- attaches the CycloneDX SBOMs (`<module>-cyclonedx{,-direct}.{json,xml}` — full + direct
  scopes × JSON + XML) and Sigstore bundles (`*.sigstore.json`) as GitHub Release assets
  on the `v*` tag.

> **One-shot.** A tag/publish is not re-runnable against the same version. If the Central Portal
> deployment stalls in a `VALIDATED` (not `PUBLISHED`) state, finish it with the manual **Publish**
> button in the Central Portal UI. To redo a failed release, bump to the next version and re-tag.

## Manual

For a local end-to-end dry run without pushing anything:

```bash
# Stage signed (or snapshot) artifacts into the local Maven repo — no upload:
./gradlew publishToMavenCentralLocal       # ~/.m2/repository
# Provide creds/signing the same way CI does, via ORG_GRADLE_PROJECT_* env vars.
```

`publishAndReleaseToMavenCentral` is the upload-and-promote task the release workflow runs; prefer
the tag-driven workflow over invoking it by hand so the tag/version guard always runs.

## How publication is wired (don't accidentally break it)

Publishing only works because four independent layers are all in place — removing any one silently
disables or mis-targets it (each is documented at its site):

1. **`enablePublication = true`** in the root `build.gradle.kts` `fkcSetupRaw` config — the harness
   gate (`SetupPublication`); without it every module skips publication.
2. **The vanniktech plugin is `alias(...)`-applied in each library module** (`fluxo-core`,
   `fluxo-common`, `fluxo-data`) and declared `apply false` at the root. The harness *configures*
   but never *applies* it; Gradle's plugin classloader isolation means it must be on each module's
   classpath, with the root `apply false` loading the classes into the shared root classloader.
3. **The `version` catalog key** (not a project-specific key) — the harness reads `version` /
   `versionName` / `app` for the publication version; a wrong key falls through to `unspecified`.
4. **Per-module `projectName`** in each module's config block — sets the Maven `artifactId`. Without
   it all modules inherit the root project name and collide on one coordinate.

## Verification

```bash
# Released version visible on Central:
curl -s https://repo1.maven.org/maven2/io/github/fluxo-kt/fluxo-core/maven-metadata.xml
# Snapshot metadata:
curl -s https://central.sonatype.com/repository/maven-snapshots/io/github/fluxo-kt/fluxo-core/maven-metadata.xml
```

Always read the live `maven-metadata.xml` to learn the current published version — never assume it
from git, which can lead the published artifacts.

## Supply-chain verification (Sigstore + CycloneDX SBOM)

Each release attaches two supply-chain assets per published artefact to the GitHub Release:

- **Sigstore bundle** (`<artifact>-<version>.sigstore.json`) — keyless signature over the JAR/POM,
  produced by `dev.sigstore.sign` against the GitHub OIDC identity of the release workflow. Verifies
  that the artefact was built by `release.yml` running on a `v*` tag of this repository.
- **CycloneDX SBOM** (`<module>-cyclonedx.json`) — Software Bill of Materials covering direct and
  transitive runtime dependencies of each library module, in CycloneDX JSON 1.5+.

### Verify a release's Sigstore bundle

```bash
# Download a JAR + its bundle from the GH Release page (or Maven Central) and run:
cosign verify-blob \
  --bundle fluxo-core-X.Y.Z.jar.sigstore.json \
  --certificate-identity-regexp '^https://github\.com/fluxo-kt/fluxo/\.github/workflows/release\.yml@refs/tags/v' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  fluxo-core-X.Y.Z.jar
```

The `--certificate-identity-regexp` anchors the signature to `release.yml` on a `v*` tag — a bundle
signed by any other workflow or branch will fail verification. Requires
[cosign](https://docs.sigstore.dev/cosign/installation/) ≥ 2.0.

### Inspect a CycloneDX SBOM

```bash
# Pretty-print the dependency tree (requires jq):
jq -r '.components[] | "\(.purl // .name)@\(.version)"' fluxo-core-cyclonedx.json | sort -u
# Or feed it to any CycloneDX-compatible scanner (e.g. trivy, syft, dependency-track).
```

SBOMs are produced by the `org.cyclonedx.bom` Gradle plugin (`cyclonedxBom` task) running against
each library module. The plugin emits two scopes — full transitive and direct-only — in both JSON
and XML; the workflow uploads them as `<module>-cyclonedx.{json,xml}` (full transitive) and
`<module>-cyclonedx-direct.{json,xml}` (direct deps only). Choose the scope you need: direct for
supply-chain review of fluxo's own declared deps, full for downstream-scanner ingestion.

## Dependency-verification metadata (build-side supply-chain)

`gradle/verification-metadata.xml` pins SHA-256 of every artefact in the resolved graph (direct +
transitive, across every KMP target's klib + every Android variant). With `verify-metadata=true`
the daemon refuses any artefact whose hash isn't pre-recorded — repo/CDN poisoning of any pin in
the catalog is rejected at resolve time, not after the build has compromised the workstation.

PGP signature pinning (`verify-signatures=true`) is intentionally left advisory for now: it would
require curating the trusted-key list for every publisher (jetbrains, google, sonatype-central,
…) and rotating it on each vendor's key roll. SHA-256-only defends against substitution; PGP is a
follow-up RFC.

### When to regenerate

Whenever the resolved graph changes:

- Catalog version bumps (`gradle/libs.versions.toml`)
- Build script dependency edits
- Wrapper bumps (`gradle-wrapper.properties`)
- New plugin additions

Dependabot PRs regenerate automatically via `.github/workflows/verify-metadata.yml` (the workflow
is gated on `dependabot[bot]` only — human contributors regen locally on the PR branch). The same
command applies before cutting a release tag, so the published graph matches the resolved one:

```bash
./gradlew --write-verification-metadata sha256 help build apiCheck cyclonedxBom \
  --no-configuration-cache -DDISABLE_TESTS
```

The `help build apiCheck cyclonedxBom` task list covers configuration, JVM/KMP compile, ABI
resolution, and the SBOM POM-enumeration path. Omitting `cyclonedxBom` leaves transitive POMs
unpinned and breaks the release-time SBOM step under strict verification. If you forget,
`./gradlew help` fails fast with "Dependency verification failed for…" citing the offending
coordinate.

### Disabling temporarily (not recommended)

If a critical artefact must be unblocked while a fix is in flight, prefer regenerating over
turning verification off. As a last resort, pass `--dependency-verification lenient` to a single
invocation (logs warnings, does not fail) — never flip `verify-metadata` to `false` in the
checked-in file.
