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

**One RC deploy at a time per branch** — `release-2-deploy-rc.yml` uses a `concurrency` group
scoped to the branch ref. If a new push lands on `prod/x.y.z` while a previous run is still
in progress, GitHub automatically cancels the older run before starting the new one. This
prevents the same `IOS_BUILD_NUMBER` or `ANDROID_VERSION_CODE` being consumed by two
simultaneous runs (one of which would fail anyway).

```yaml
concurrency:
  group: rc-deploy-${{ github.ref }}   # unique per prod/* branch
  cancel-in-progress: true
```

> This was added after the v1.0.0 RC cycle where two runs (#4 and #5) were triggered back-to-back
> by rapid successive pushes to `prod/1.0.0` and ran in parallel, each incrementing build numbers
> independently.

### How iOS build number stamping works (important — read before modifying)

`distribute-testflight.yml` runs these three steps **in order, in the same job**:

```
1. Set iOS build number   →  reads IOS_BUILD_NUMBER (e.g. 13)
                              computes NEXT = 14
                              writes 14 back to GitHub Variables via gh api
                              exports IOS_BUILD_NUMBER=14 to $GITHUB_ENV

2. Build iOS App          →  fastlane build_release
                              set_info_plist_value patches iosApp/Info.plist
                              CFBundleVersion → 14   ← happens BEFORE build_app
                              IPA is baked with build number 14 ✓

3. Upload to TestFlight   →  fastlane upload_testflight
                              uploads the IPA (CFBundleVersion=14)
                              Apple accepts: 14 > previous ✓
```

**Why `set_info_plist_value` and NOT `increment_build_number(xcodeproj:)`:**

Fastlane's `increment_build_number(xcodeproj:)` internally calls `agvtool`, which requires
`CURRENT_PROJECT_VERSION` to be declared in the target's Xcode build settings. If it is
absent the call is a **silent no-op** — Fastlane prints nothing, the plist is never updated,
and every IPA after the first RC is rejected by Apple with:

```
The bundle version must be higher than the previously uploaded version
```

This is exactly what happened during the v1.0.0 RC cycle: the Huezoo xcodeproj was missing
`CURRENT_PROJECT_VERSION` (unlike Krail, which always had it). CI incremented `IOS_BUILD_NUMBER`
correctly (7 → 8 → 9 … → 13) but every IPA was still built with the hardcoded plist value `7`,
causing repeated Apple rejections.

**Fix applied (April 2026):**

1. `Fastfile` — replaced `increment_build_number(xcodeproj:)` with `set_info_plist_value`
   which patches `iosApp/Info.plist → CFBundleVersion` directly before `build_app` runs.
   No xcodeproj dependency; works regardless of `CURRENT_PROJECT_VERSION` being present.

2. `project.pbxproj` — added `CURRENT_PROJECT_VERSION = 13` to both Debug and Release
   target build configs (mirrors Krail). Enables `increment_build_number` as a future fallback.

3. `Info.plist` — bumped hardcoded `CFBundleVersion` from `7` to `13` to match the GitHub
   variable state after the failed RC attempts.

**Krail vs Huezoo diff that caused the bug:**

| Setting | Krail | Huezoo (before fix) |
|---|---|---|
| `CURRENT_PROJECT_VERSION` in xcodeproj | ✅ `= 11` | ❌ missing |
| `GENERATE_INFOPLIST_FILE` | not set | `YES` |
| `increment_build_number(xcodeproj:)` result | Works correctly | Silent no-op |
| IPA build number source | CI variable ✓ | Hardcoded plist value ✗ |

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
