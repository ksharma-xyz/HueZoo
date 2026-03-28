# Huezoo Release Process

## Overview

```
main (versionName = 1.0.0)
  │
  │  ← development happens here
  │
  ├─ Cut Release Branch ──► prod/1.0.0 ──[fix]──[fix]
  │         │                    │            │      │
  │  main bumped to 1.1.0    RC1 tag      RC2 tag  RC3 tag
  │                               │            │      │
  │                           GP Internal  GP Internal  GP Internal
  │                           TF build     TF build     TF build
  │
  │                          (happy with RC3? run Tag Release)
  │
  │  ◄── Tag Release creates v1.0.0 tag + draft GitHub Release
  │
  │  Manual promotion:
  │    Android → promote in Google Play Console
  │    iOS     → submit in App Store Connect
  │
main (versionName = 1.1.0)   ← already bumped when branch was cut
```

Both **Android** and **iOS** are fully automated on every push to `prod/**`.

**Key principle**: `main` always carries the *next* version. `versionName` is bumped
automatically on `main` right when the release branch is cut — you never bump it manually.

---

## Workflows at a Glance

| Workflow file | UI name | Trigger | What it does |
|---|---|---|---|
| `build.yml` | Huezoo App CI | Push to `main`, PRs | Quality gate + debug/release build + Firebase distribution |
| `release-1-cut.yml` | 1. Cut Release Branch | Manual | Creates `prod/{version}` branch + bumps `main` to next version |
| `release-2-deploy-rc.yml` | 2. Deploy RC | Auto on `prod/**` push | Android RC tag + Google Play Internal + iOS TestFlight |
| `release-3-tag.yml` | 3. Tag Release | Manual | Creates final `v{version}` tag |
| `create-github-release.yml` | Create GitHub Release | Auto on `v*` tag push (non-RC), or manual | Creates draft GitHub Release |
| `distribute-testflight.yml` | Distribute TestFlight | Auto (called by release-2) or Manual | iOS build + TestFlight upload |

---

## Step-by-Step: Starting a Release

### 1. Kick off the release branch

`main` already has the correct `versionName` (bumped automatically when the previous
release branch was cut). Just go to **Actions → 1. Cut Release Branch → Run workflow**.

| Field | Example |
|---|---|
| Base branch | `main` (default) |
| Next version | *(leave blank)* auto-increments minor: `1.1.0` |

This workflow:
- Reads `versionName` from `main`'s `androidApp/build.gradle.kts` (e.g. `1.0.0`)
- Creates and pushes `prod/1.0.0`
- Immediately bumps `versionName` on `main` to `1.1.0` (commit tagged `[skip ci]`)
- Pushing the branch automatically triggers `release-2-deploy-rc.yml`

### 2. Watch the automatic release pipeline

`release-2-deploy-rc.yml` runs on every push to `prod/**`:

```
code-quality
    └── build-android-release (signed AAB)
            └── tag-release-candidate  →  creates v1.0.0-RC1
                    └── distribute-google-play  →  uploads to GP Internal track

distribute-testflight  →  builds IPA + uploads to TestFlight  (runs in parallel)
```

The first push creates `v1.0.0-RC1`. Every subsequent push to the same branch
increments the counter: `v1.0.0-RC2`, `v1.0.0-RC3`, etc.

### 3. Test on Google Play Internal and TestFlight

- Install via Google Play internal track and TestFlight
- Validate both Android and iOS builds
- If a fix is needed: commit to `prod/1.0.0` and push → automatic RC bump + new GP + TF upload

---

## Step-by-Step: Shipping to Production

Once internal testing passes:

### 1. Tag the release

Run **Actions → 3. Tag Release → Run workflow**.

| Field | Example |
|---|---|
| Version | `1.0.0` |

This workflow:
1. Validates `prod/1.0.0` exists
2. Creates and pushes the final `v1.0.0` tag
3. Pushing the tag automatically triggers `create-github-release.yml` → draft GitHub Release

### 2. Promote to production (manual)

- **Android**: Google Play Console → your app → the latest internal build → Promote to Production
- **iOS**: App Store Connect → TestFlight → the latest build → Submit for App Store review

### 3. Publish the GitHub Release draft

Review and publish the draft created by `create-github-release.yml`.

---

## Branching Convention

| Branch | Purpose | CI/CD triggered |
|---|---|---|
| `main` | Active development | Build + Firebase distribution |
| `prod/{version}` | Release stabilisation | Android RC + Google Play Internal + iOS TestFlight |
| `{date}-{description}` | Feature/fix branches | Nothing (only on PR to main) |

**Never commit directly to `main` for a release.** Always go through a `prod/*` branch so
the RC tagging and Google Play upload pipeline runs.

---

## Versioning

### Android

| Property | Where | Who updates it |
|---|---|---|
| `versionName` | `androidApp/build.gradle.kts` | `release-1-cut.yml` (bumps `main` immediately after cutting the branch) |
| `versionCode` | GitHub repo variable `ANDROID_VERSION_CODE` | CI (increments on every release build) |

Each release build reads `ANDROID_VERSION_CODE`, increments it by 1, writes the new
value back to the variable, and passes it to Gradle. Debug builds read the value
without incrementing. The counter survives workflow renames because it lives in GitHub
Settings, not inside any workflow file.

### iOS

| Property | Where | Who updates it |
|---|---|---|
| `CFBundleShortVersionString` | `iosApp/iosApp/Info.plist` | `release-1-cut.yml` (bumps `main` after cutting the branch) |
| `CFBundleVersion` | GitHub repo variable `IOS_BUILD_NUMBER` | CI (increments on every TestFlight upload) |

`CFBundleVersion` must be globally and strictly increasing across all App Store / TestFlight
uploads — it does **not** reset when the marketing version changes.

### Git tags

| Pattern | Meaning | Created by |
|---|---|---|
| `v1.0.0-RC1` | First release candidate | `release-2-deploy-rc.yml` (automatic) |
| `v1.0.0-RC2` | Second RC after a fix | `release-2-deploy-rc.yml` (automatic) |
| `v1.0.0` | Final production release | `release-3-tag.yml` (manual) |

---

## Google Play Tracks

| Track | Use for | How to target |
|---|---|---|
| `internal` | Team dogfooding during RC cycle | Automatic via `release-2-deploy-rc.yml` |
| `production` | Full release | Google Play Console → promote from internal |

**There is no CI workflow for production upload.** Promoting from internal to production
is done manually in Google Play Console — this gives explicit human sign-off before
anything reaches users.

---

## Hotfixes During RC / After Shipping

**Fix flow: `main` first, then cherry-pick to `prod/*`.**

Never commit a fix directly only to the prod branch — it would be lost when the next
release cycle starts from `main`.

1. Fix the bug on `main` (commit normally, goes through PR/review as usual)
2. Cherry-pick the fix commit onto the prod branch:
   ```
   git cherry-pick <commit-sha>
   git push origin prod/1.0.0
   ```
3. Push to `prod/1.0.0` triggers `release-2-deploy-rc.yml` → new RC tag → GP Internal + TestFlight
4. Validate on both platforms, then run `release-3-tag.yml` when ready

---

## Checklist Before Running `release-3-tag.yml`

- [ ] Latest RC has been installed from Google Play Internal and validated
- [ ] Latest TestFlight build validated on iOS
- [ ] All required commits are on `prod/{version}` (no pending fixes)
- [ ] Release notes / changelog reviewed

---

## One-Time Setup: GitHub Repository Variables

### ANDROID_VERSION_CODE

1. Go to GitHub → Settings → Secrets and variables → Actions → **Variables** tab
2. Click **New repository variable**
   - Name: `ANDROID_VERSION_CODE`
   - Value: `1` *(start from 1, or higher than any manually uploaded versionCode)*
3. Done — all future release builds will auto-increment this and write it back

### IOS_BUILD_NUMBER

1. Go to GitHub → Settings → Secrets and variables → Actions → **Variables** tab
2. Click **New repository variable**
   - Name: `IOS_BUILD_NUMBER`
   - Value: `1` *(start from 1, or higher than any previous TestFlight upload)*
3. Done — all future TestFlight uploads will auto-increment this and write it back

### DEVELOPMENT_TEAM

1. Go to GitHub → Settings → Secrets and variables → Actions → **Variables** tab
2. Click **New repository variable**
   - Name: `DEVELOPMENT_TEAM`
   - Value: *(your Apple Developer Team ID — find it in developer.apple.com → Membership)*

---

## Required GitHub Secrets

Configure all secrets in GitHub → Settings → Secrets and variables → Actions → **Secrets** tab.

| Secret | Used by | Notes |
|---|---|---|
| `PAT_HUEZOO_GITHUB` | RC/final tag creation, GitHub Release, version code write-back | Classic PAT with `repo` scope, or fine-grained PAT with `Contents: Read and write` + `Variables: Read and write` |
| `ANDROID_KEYSTORE_FILE` | Android AAB signing | Base64-encoded `.jks` file |
| `ANDROID_KEYSTORE_PASSWORD` | Android AAB signing | |
| `ANDROID_KEY_ALIAS` | Android AAB signing | |
| `ANDROID_KEY_PASSWORD` | Android AAB signing | |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Google Play upload | JSON key from Google Play Console service account |
| `APPSTORE_PRIVATE_KEY` | iOS TestFlight | `.p8` key content from App Store Connect |
| `APPSTORE_KEY_ID` | iOS TestFlight | Key ID from App Store Connect |
| `APPSTORE_ISSUER_ID` | iOS TestFlight | Issuer ID from App Store Connect |
| `IOS_DIST_SIGNING_KEY_BASE64` | iOS code signing | Base64-encoded `.p12` distribution certificate |
| `IOS_DIST_SIGNING_KEY_PASSWORD` | iOS code signing | Password for the `.p12` file |
| `IOS_PROVISIONING_PROFILE_NAME` | iOS provisioning profile | Name as shown in Xcode / Apple Developer portal |

**Firebase secrets** (required once Firebase is integrated — Phase 8):

| Secret | Used by |
|---|---|
| `FIREBASE_GOOGLE_SERVICES_JSON_DEBUG` | Android Firebase (debug) |
| `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE` | Android Firebase (release) |
| `FIREBASE_IOS_GOOGLE_INFO` | iOS Firebase |
| `FIREBASE_SERVICE_ACCOUNT_KEY` | Firebase App Distribution |
| `FIREBASE_ANDROID_DEBUG_APP_ID` | Firebase App Distribution (debug) |
| `FIREBASE_ANDROID_PROD_APP_ID` | Firebase App Distribution (release) |

---

## Required GitHub Variables

| Variable | Used by |
|---|---|
| `ANDROID_VERSION_CODE` | Android versionCode (auto-incremented on every release build) |
| `IOS_BUILD_NUMBER` | iOS CFBundleVersion (auto-incremented on every TestFlight upload) |
| `DEVELOPMENT_TEAM` | iOS Xcode signing (Apple Team ID) |

---

## One-Time iOS Setup (Before First TestFlight Upload)

1. **Set Bundle Identifier** in Xcode: open `iosApp/iosApp.xcodeproj` → iosApp target →
   General → Bundle Identifier → set to `xyz.ksharma.huezoo`

2. **Add version fields to Info.plist**: open `iosApp/iosApp/Info.plist` and add:
   ```xml
   <key>CFBundleShortVersionString</key>
   <string>1.0.0</string>
   <key>CFBundleVersion</key>
   <string>1</string>
   ```
   These are required for `release-1-cut.yml` to bump the marketing version and for
   Fastlane's `increment_build_number` to work.

3. **Create App Store Connect app**: register `xyz.ksharma.huezoo` in App Store Connect.

4. **Generate App Store Connect API key**: App Store Connect → Users and Access → Integrations →
   App Store Connect API → generate a key with App Manager role. Save the `.p8` file,
   Key ID, and Issuer ID as GitHub secrets.

5. **Create distribution certificate + provisioning profile** in Apple Developer portal.
   Export the `.p12` and note the provisioning profile name.

---

## Troubleshooting

**RC tag already pushed but Google Play upload failed**
Re-run only the `distribute-google-play` job from the failed workflow run
(GitHub Actions → re-run failed jobs).

**`release-1-cut.yml` fails with "branch already exists"**
The branch `prod/{version}` was already created. Push directly to that branch
or choose a new version number.

**`release-3-tag.yml` fails with "tag already exists"**
Delete the tag manually: `git push origin :refs/tags/v{version}` then re-run.

**TestFlight upload rejected: bundle version must be higher**
Update `IOS_BUILD_NUMBER` in GitHub → Settings → Variables to a value higher
than the last uploaded `CFBundleVersion`.

**`increment_build_number` fails in Fastlane**
`CURRENT_PROJECT_VERSION` is not set in the Xcode project. Open Xcode,
go to the iosApp target → Build Settings → search "Current Project Version"
and set it to `1`. Then commit the updated `.pbxproj`.

**Android release build fails: `keystore.jks` not found**
The `ANDROID_KEYSTORE_FILE` secret is not set or is incorrectly base64-encoded.
Encode it with: `base64 -i your-keystore.jks | pbcopy`

---

## CI/CD Status

### Android — ⬜ Not yet end-to-end tested

Requires one-time setup:
- [ ] Keystore generated and secrets configured
- [ ] `ANDROID_VERSION_CODE` repo variable created
- [ ] `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret configured
- [ ] App registered in Google Play Console

### iOS — ⬜ Not yet end-to-end tested

Requires one-time setup:
- [ ] Bundle Identifier set to `xyz.ksharma.huezoo` in Xcode
- [ ] `CFBundleShortVersionString` / `CFBundleVersion` added to `Info.plist`
- [ ] `CURRENT_PROJECT_VERSION` set in Xcode build settings
- [ ] App Store Connect app created
- [ ] API key, distribution cert, and provisioning profile configured
- [ ] `IOS_BUILD_NUMBER` and `DEVELOPMENT_TEAM` repo variables created
