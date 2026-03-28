# CI/CD Architecture

## Architecture Overview

```
┌─────────────────┐
│   code-quality  │  ← Detekt static analysis
└─────────┬───────┘
          │ (blocks until passes)
          ▼
  ┌───────────────────────────────────┐
  │        Build Jobs (Parallel)      │
  ├─────────────┬─────────────┬───────┤
  │build-android│build-android│build  │
  │   -debug    │  -release   │ -ios  │
  └─────────────┴─────────────┴───────┘
          │           │
          ▼           ▼
  ┌─────────────┬─────────────┐
  │ distribute  │ distribute  │    ← only on push to main
  │  -debug     │  -release   │
  │  Firebase   │  Firebase   │
  └─────────────┴─────────────┘
```

---

## Workflow Files

| File | Trigger | Responsibility |
|---|---|---|
| `build.yml` | Push to `main`, PRs to `main`/`prod/*` | Main orchestration |
| `code-quality.yml` | Called by `build.yml`, `release-2-deploy-rc.yml` | Detekt |
| `build-android.yml` | Called by orchestrators | Android debug + release build |
| `build-ios.yml` | Called by orchestrators | iOS debug build (no signing) |
| `distribute-firebase.yml` | Called by `build.yml` (main push only) | Firebase App Distribution |
| `distribute-google-play.yml` | Called by `release-2-deploy-rc.yml` or manual | Google Play Internal upload |
| `distribute-testflight.yml` | Called by `release-2-deploy-rc.yml` or manual | iOS build + TestFlight upload |
| `release-1-cut.yml` | Manual | Create `prod/{version}` branch + bump `main` |
| `release-2-deploy-rc.yml` | Auto on `prod/**` push | RC tag + GP Internal + TestFlight |
| `release-3-tag.yml` | Manual | Create final `v{version}` tag |
| `create-github-release.yml` | Auto on `v*` tag (non-RC), or manual | Draft GitHub Release |

---

## Release Pipeline

```
[Manual] 1. Cut Release Branch
   → creates prod/1.0.0, bumps main to 1.1.0

[Auto] 2. Deploy RC  (triggered by every push to prod/1.0.0)
   code-quality
       └── build-android-release
               └── tag-release-candidate  →  v1.0.0-RC1
                       └── distribute-google-play  →  Internal track

   distribute-testflight  →  iOS build → TestFlight  (parallel)

[Manual] 3. Tag Release
   → creates v1.0.0

[Auto] Create GitHub Release
   → draft release with auto-generated notes

[Manual] Promote to Production
   Android → Google Play Console
   iOS     → App Store Connect
```

---

## Design Principles

**Single responsibility per workflow file** — build logic is separate from distribution logic. Changes to one don't affect the other.

**Quality gate before builds** — Detekt must pass before any build starts. Fail fast, save CI minutes.

**Parallel builds** — Android debug, Android release, and iOS build all run simultaneously after the quality gate.

**Independent checkouts** — each job checks out the repo independently. This is intentional and follows industry standard practice (GitHub, Google, Microsoft all use this pattern). Checkout is fast (~15-30s with shallow clones and CDN caching); sharing via artifacts adds complexity for minimal gain.

**Production promotion is always manual** — CI never pushes to Google Play production or submits to App Store review. Human sign-off required.

**Monotonically increasing build numbers** — `ANDROID_VERSION_CODE` and `IOS_BUILD_NUMBER` are stored as GitHub repository variables, auto-incremented by CI on each release/TestFlight build, and written back via the GitHub API. They survive workflow renames because they live in Settings, not in workflow files.

---

## Environments

The `Firebase` GitHub environment gates access to secrets related to signing, distribution,
and Firebase. All jobs that need secrets are in this environment.

---

## Firebase App Distribution

On every push to `main`, after a successful build:
- Debug APK → distributed to `Internal` group
- Release APK → distributed to `Friends` group

This gives continuous delivery of every merged commit without going through Google Play.

*Note: Firebase App Distribution requires a Firebase project to be set up (Phase 8). The
workflows are in place — activate by configuring the required secrets.*

---

## Secrets vs Variables

**Secrets** (encrypted, never logged): signing keys, API keys, service account JSON, passwords.

**Variables** (plain text, visible in logs): `ANDROID_VERSION_CODE`, `IOS_BUILD_NUMBER`,
`DEVELOPMENT_TEAM`. These are counters and IDs that CI reads and writes back, not sensitive data.
