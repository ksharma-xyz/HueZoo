# CI/CD Setup Checklist

Run this checklist once before the first CI pipeline execution. For full context on each step see `docs/release/RELEASE_PROCESS.md`.

---

## GitHub Repository Secrets

Go to **GitHub → Settings → Secrets and variables → Actions → Secrets tab**.

### Android signing

- [ ] `ANDROID_KEYSTORE_FILE` — base64-encoded `.jks` keystore file
  ```
  base64 -i your-keystore.jks | pbcopy
  ```
- [ ] `ANDROID_KEYSTORE_PASSWORD` — keystore password
- [ ] `ANDROID_KEY_ALIAS` — key alias inside the keystore
- [ ] `ANDROID_KEY_PASSWORD` — key password

### Google Play

- [ ] `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` — JSON key from a Google Play Console service account with Release Manager role

### iOS signing

- [ ] `IOS_DIST_SIGNING_KEY_BASE64` — base64-encoded `.p12` Apple Distribution certificate
  ```
  base64 -i distribution.p12 | pbcopy
  ```
- [ ] `IOS_DIST_SIGNING_KEY_PASSWORD` — password for the `.p12` file
- [ ] `IOS_PROVISIONING_PROFILE_NAME` — name exactly as it appears in Apple Developer portal (e.g. `Huezoo App Store`)

### App Store Connect API (TestFlight)

- [ ] `APPSTORE_KEY_ID` — Key ID from App Store Connect → Users and Access → Integrations → App Store Connect API
- [ ] `APPSTORE_ISSUER_ID` — Issuer ID from the same page
- [ ] `APPSTORE_PRIVATE_KEY` — contents of the downloaded `.p8` file

### GitHub PAT

- [ ] `PAT_HUEZOO_GITHUB` — Personal Access Token with `repo` scope (classic PAT) or fine-grained PAT with:
  - `Contents: Read and write`
  - `Actions variables: Read and write`

  Used for: RC tag creation, version code write-back, GitHub Release creation.

### Firebase (Phase 8 — uncomment workflow steps when ready)

- [ ] `FIREBASE_SERVICE_ACCOUNT_KEY` — Firebase service account JSON for App Distribution
- [ ] `FIREBASE_ANDROID_DEBUG_APP_ID` — Firebase Android app ID (debug variant)
- [ ] `FIREBASE_ANDROID_PROD_APP_ID` — Firebase Android app ID (release variant)
- [ ] `FIREBASE_GOOGLE_SERVICES_JSON_DEBUG` — base64-encoded `google-services.json` (debug)
- [ ] `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE` — base64-encoded `google-services.json` (release)
- [ ] `FIREBASE_IOS_GOOGLE_INFO` — base64-encoded `GoogleService-Info.plist`

---

## GitHub Repository Variables

Go to **GitHub → Settings → Secrets and variables → Actions → Variables tab**.

- [ ] `ANDROID_VERSION_CODE` — start at `1` (or higher than any manually uploaded versionCode)
- [ ] `IOS_BUILD_NUMBER` — start at `1` (or higher than any previous TestFlight upload)
- [ ] `DEVELOPMENT_TEAM` — Apple Developer Team ID (find it at developer.apple.com → Membership)

---

## One-Time iOS Xcode Setup

Do this in Xcode before the first TestFlight upload:

- [ ] Open `iosApp/iosApp.xcodeproj` → iosApp target → **General → Identity**
  - Set **Bundle Identifier** to `xyz.ksharma.huezoo`
  - Set **Version** to `1.0.0`
  - Set **Build** to `1`

- [ ] Add `CFBundleShortVersionString` and `CFBundleVersion` to `iosApp/iosApp/Info.plist`:
  ```xml
  <key>CFBundleShortVersionString</key>
  <string>1.0.0</string>
  <key>CFBundleVersion</key>
  <string>1</string>
  ```
  Required for `release-1-cut.yml` to auto-bump the marketing version and for Fastlane's `increment_build_number` to work.

- [ ] In Xcode → iosApp target → **Build Settings** → search **"Current Project Version"** → set to `1`

- [ ] Register `xyz.ksharma.huezoo` in **App Store Connect** (create the app record)

- [ ] Create an **App Store Distribution Certificate** and **App Store Provisioning Profile** in the Apple Developer portal. Export the `.p12` and note the profile name for the secrets above.

---

## Verify

Once all secrets and variables are set, trigger the first run:

1. Push any commit to `main` → confirm **Huezoo App CI** workflow passes (Detekt + Android debug build + iOS build)
2. Manually trigger **Distribute TestFlight** to validate iOS signing end-to-end before the first real release
3. Run **1. Cut Release Branch** when ready to ship
