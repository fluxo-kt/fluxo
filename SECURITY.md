# Security policy

## Supported versions

Fluxo is pre-1.0: only the latest release receives security fixes, and a fix ships as a new release, not as a
patch to an older line.

## Reporting a vulnerability

Report privately through GitHub:
[Security → Report a vulnerability](https://github.com/fluxo-kt/fluxo/security/advisories/new).
Do not open a public issue, pull request or discussion for it.

Include the affected version or commit, the platform (JVM, Android, JS, Wasm or Native target), and a minimal
reproduction or the code path involved.

## What happens next

1. The report is confirmed or declined in the private advisory thread.
2. A confirmed vulnerability is fixed on `dev` and released.
3. The advisory is then published with the fixed version, and a CVE is requested through GitHub when it applies.
   Reporters are credited unless they ask not to be.
