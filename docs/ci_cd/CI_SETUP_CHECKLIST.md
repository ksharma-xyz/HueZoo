# CI/CD Setup Checklist

Run this checklist once before the first CI pipeline execution.
All secrets go to: **github.com/ksharma-xyz/HueZoo → Settings → Secrets and variables → Actions → Secrets tab**
All variables go to: **same page → Variables tab**

---

## Status

| Secret / Variable | Status |
|---|---|
| `ANDROID_KEYSTORE_FILE` | ✅ Done |
| `ANDROID_KEYSTORE_PASSWORD` | ✅ Done |
| `ANDROID_KEY_ALIAS` | ✅ Done |
| `ANDROID_KEY_PASSWORD` | ✅ Done |
| `FIREBASE_GOOGLE_SERVICES_JSON_DEBUG` | ✅ Done |
| `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE` | ✅ Done |
| `FIREBASE_IOS_GOOGLE_INFO` | ⬜ |
| `FIREBASE_SERVICE_ACCOUNT_KEY` | ⬜ |
| `FIREBASE_ANDROID_DEBUG_APP_ID` | ⬜ |
| `FIREBASE_ANDROID_PROD_APP_ID` | ⬜ |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | ⬜ |
| `APPSTORE_KEY_ID` | ⬜ |
| `APPSTORE_ISSUER_ID` | ⬜ |
| `APPSTORE_PRIVATE_KEY` | ⬜ |
| `IOS_DIST_SIGNING_KEY_BASE64` | ⬜ |
| `IOS_DIST_SIGNING_KEY_PASSWORD` | ⬜ |
| `IOS_PROVISIONING_PROFILE_NAME` | ⬜ |
| `PAT_HUEZOO_GITHUB` | ⬜ |
| `ANDROID_VERSION_CODE` (variable) | ⬜ |
| `IOS_BUILD_NUMBER` (variable) | ⬜ |
| `DEVELOPMENT_TEAM` (variable) | ⬜ |

---

## Secrets — Step by Step

### ✅ Android Signing (already done)

`ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` — all set.

---

### ✅ Firebase Google Services JSON (already done)

`FIREBASE_GOOGLE_SERVICES_JSON_DEBUG` and `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE` — both set.

How they were created (for reference):
```bash
base64 -i androidApp/src/debug/google-services.json | pbcopy
# → paste as FIREBASE_GOOGLE_SERVICES_JSON_DEBUG

base64 -i androidApp/src/release/google-services.json | pbcopy
# → paste as FIREBASE_GOOGLE_SERVICES_JSON_RELEASE
```

---

### ⬜ FIREBASE_IOS_GOOGLE_INFO

**What:** base64-encoded `GoogleService-Info.plist` for the iOS app.

**Steps:**
```bash
base64 -i iosApp/GoogleService-Info.plist | pbcopy
```
Paste the copied value as secret `FIREBASE_IOS_GOOGLE_INFO`.

---

### ⬜ FIREBASE_SERVICE_ACCOUNT_KEY

**What:** Firebase service account JSON — used by Firebase App Distribution CI workflow.

**Steps:**
1. Go to [console.firebase.google.com](https://console.firebase.google.com) → Huezoo project
2. Click the gear icon → **Project Settings**
3. Go to **Service accounts** tab
4. Click **Generate new private key** → **Generate key**
5. A `.json` file downloads — base64 encode and add as secret:
```bash
base64 -i path/to/downloaded-key.json | pbcopy
# → paste as FIREBASE_SERVICE_ACCOUNT_KEY
```

---

### ⬜ FIREBASE_ANDROID_DEBUG_APP_ID + FIREBASE_ANDROID_PROD_APP_ID

**What:** Firebase App ID for the Android app (looks like `1:123456789:android:abcdef123456`).

**Steps:**
1. Firebase Console → Huezoo project → **Project Settings** (gear icon)
2. Scroll to **Your apps** section
3. Click the Android app
4. Copy the **App ID** field
5. Add as `FIREBASE_ANDROID_DEBUG_APP_ID` (same value for both if you only have one Android app — or create two Firebase Android apps for debug/release variants)

---

### ⬜ GOOGLE_PLAY_SERVICE_ACCOUNT_JSON

**What:** Google Play Console service account JSON — used to upload AAB to Play Store via CI.

**Steps:**
1. Go to [play.google.com/console](https://play.google.com/console) → **Setup → API access**
2. Click **Link to a Google Cloud project** (or use existing)
3. In Google Cloud Console → **IAM & Admin → Service Accounts → Create Service Account**
   - Name: `huezoo-ci`
   - Role: `Service Account User`
4. Back in Play Console → **Grant access** to the service account → Role: **Release Manager**
5. In Google Cloud Console → Service account → **Keys → Add key → Create new key → JSON**
6. A `.json` file downloads — paste the **raw JSON content** (not base64) as secret:
   ```
   GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
   ```
   > Note: this secret is used as plain JSON text by the `r0adkll/upload-google-play` action, not base64.

---

### ⬜ APPSTORE_KEY_ID + APPSTORE_ISSUER_ID + APPSTORE_PRIVATE_KEY

**What:** App Store Connect API key — used by Fastlane to upload to TestFlight without 2FA prompts.

**Steps:**
1. Go to [appstoreconnect.apple.com](https://appstoreconnect.apple.com) → **Users and Access**
2. Click **Integrations** tab → **App Store Connect API**
3. Click **+** to create a new key
   - Name: `Huezoo CI`
   - Access: **App Manager**
4. Download the `.p8` file (**you can only download it once**)
5. Note the **Key ID** and **Issuer ID** shown on the page
6. Add three secrets:
   - `APPSTORE_KEY_ID` — the Key ID (e.g. `ABC123DEFG`)
   - `APPSTORE_ISSUER_ID` — the Issuer ID (UUID format)
   - `APPSTORE_PRIVATE_KEY` — raw contents of the `.p8` file:
     ```bash
     cat AuthKey_XXXXXXXX.p8 | pbcopy
     # → paste as APPSTORE_PRIVATE_KEY
     ```

---

### ⬜ IOS_DIST_SIGNING_KEY_BASE64 + IOS_DIST_SIGNING_KEY_PASSWORD

**What:** Apple Distribution certificate as a `.p12` — used by CI to sign the IPA for TestFlight.

**Steps:**
1. Open **Keychain Access** on your Mac
2. Find your **Apple Distribution: Karan Sharma** certificate (under My Certificates)
3. Right-click → **Export** → save as `distribution.p12`
4. Set a password when prompted (remember it for the next secret)
5. Add secrets:
   ```bash
   base64 -i distribution.p12 | pbcopy
   # → paste as IOS_DIST_SIGNING_KEY_BASE64
   ```
   - `IOS_DIST_SIGNING_KEY_PASSWORD` — the password you set in step 4

If you don't have an Apple Distribution certificate yet:
1. Xcode → Settings → Accounts → select your team → **Manage Certificates → + → Apple Distribution**

---

### ⬜ IOS_PROVISIONING_PROFILE_NAME

**What:** The exact name of the App Store provisioning profile — used by Fastlane to select the right profile when signing.

**Steps:**
1. Go to [developer.apple.com](https://developer.apple.com) → **Certificates, Identifiers & Profiles → Profiles**
2. Click **+** → **App Store Connect** → **App Store**
3. Select App ID: `xyz.ksharma.huezoo`
4. Select your Distribution certificate
5. Name it: `Huezoo App Store` (or anything clear)
6. Download and double-click to install it in Xcode
7. Add secret: `IOS_PROVISIONING_PROFILE_NAME` = `Huezoo App Store` (exact name from step 5)

---

### ⬜ PAT_HUEZOO_GITHUB

**What:** GitHub Personal Access Token — used to write back `ANDROID_VERSION_CODE` and `IOS_BUILD_NUMBER` variables after each build, and to create release tags.

**Steps:**
1. Go to [github.com/settings/tokens](https://github.com/settings/tokens)
2. **Classic PAT** → Generate new token (classic)
   - Note: `Huezoo CI`
   - Expiration: No expiration (or 1 year)
   - Scopes: check **`repo`** (full control of private repositories)
3. Copy the generated token
4. Add as secret `PAT_HUEZOO_GITHUB`

---

## Variables

Go to **Settings → Secrets and variables → Actions → Variables tab**.

### ⬜ ANDROID_VERSION_CODE

Start at `1`. Incremented automatically on every release build.
- Name: `ANDROID_VERSION_CODE`
- Value: `1`

### ⬜ IOS_BUILD_NUMBER

Start at `1`. Incremented automatically on every TestFlight upload.
- Name: `IOS_BUILD_NUMBER`
- Value: `1`

### ⬜ DEVELOPMENT_TEAM

Your Apple Developer Team ID.

**How to find it:**
1. Go to [developer.apple.com](https://developer.apple.com) → Account → **Membership details**
2. Copy the **Team ID** (10-character alphanumeric string)

- Name: `DEVELOPMENT_TEAM`
- Value: your Team ID from the membership page

---

## Verify

Once all secrets and variables are set:

1. Push any commit to `main` → confirm **Huezoo App CI** workflow passes (Detekt + Android debug + iOS build)
2. Manually trigger **Distribute TestFlight** to validate iOS signing end-to-end
3. Run **1. Cut Release Branch** when ready to ship v1.0
