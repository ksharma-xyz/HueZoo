# CI/CD Setup Checklist

All secrets → **github.com/ksharma-xyz/HueZoo → Settings → Secrets and variables → Actions → Secrets tab**
All variables → **same page → Variables tab**

---

## Status

| Secret / Variable | Format | Status |
|---|---|---|
| `ANDROID_KEYSTORE_FILE` | base64 | ✅ Done |
| `ANDROID_KEYSTORE_PASSWORD` | plain text | ✅ Done |
| `ANDROID_KEY_ALIAS` | plain text | ✅ Done |
| `ANDROID_KEY_PASSWORD` | plain text | ✅ Done |
| `FIREBASE_GOOGLE_SERVICES_JSON_DEBUG` | base64 | ✅ Done |
| `FIREBASE_GOOGLE_SERVICES_JSON_RELEASE` | base64 | ✅ Done |
| `FIREBASE_IOS_GOOGLE_INFO` | base64 | ✅ Done |
| `PAT_HUEZOO_GITHUB` | plain text | ✅ Done |
| `APPSTORE_KEY_ID` | plain text | ⬜ |
| `APPSTORE_ISSUER_ID` | plain text | ⬜ |
| `APPSTORE_PRIVATE_KEY` | plain text (raw .p8) | ⬜ |
| `IOS_DIST_SIGNING_KEY_BASE64` | base64 | ⬜ |
| `IOS_DIST_SIGNING_KEY_PASSWORD` | plain text | ⬜ |
| `IOS_PROVISIONING_PROFILE_NAME` | plain text | ⬜ |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | plain text (raw JSON) | ⬜ |
| `FIREBASE_SERVICE_ACCOUNT_KEY` | base64 | ⬜ |
| `FIREBASE_ANDROID_DEBUG_APP_ID` | plain text | ⬜ |
| `FIREBASE_ANDROID_PROD_APP_ID` | plain text | ⬜ |
| `ANDROID_VERSION_CODE` (variable) | plain text | ⬜ |
| `IOS_BUILD_NUMBER` (variable) | plain text | ⬜ |
| `DEVELOPMENT_TEAM` (variable) | plain text | ⬜ |

---

## Remaining Secrets

---

### APPSTORE_KEY_ID + APPSTORE_ISSUER_ID + APPSTORE_PRIVATE_KEY

**What they are:** Three values from a single API key in App Store Connect. Used by Fastlane to upload builds to TestFlight automatically — no 2FA, no password needed.

**Format:** All three are plain text — paste as-is, no base64.

**Steps:**
1. Open [appstoreconnect.apple.com](https://appstoreconnect.apple.com)
2. Click your name (top right) → **Users and Access**
3. Click the **Integrations** tab in the top nav
4. Click **App Store Connect API** in the left sidebar
5. Click the **+** button to create a new key:
   - Name: `Huezoo CI`
   - Access: **App Manager**
   - Click **Generate**
6. You now see a row with **Key ID** and **Issuer ID** at the top of the page — copy both
7. Click **Download API Key** — downloads a file named `AuthKey_XXXXXXXXXX.p8`
   > ⚠️ You can only download this file **once**. Store it safely.
8. Add the three secrets:

   **`APPSTORE_KEY_ID`** — the Key ID shown on the page (e.g. 10 characters like `ABC123DEFG`)
   → paste it directly as plain text

   **`APPSTORE_ISSUER_ID`** — the Issuer ID shown at the top of the page (UUID format)
   → paste it directly as plain text

   **`APPSTORE_PRIVATE_KEY`** — the full contents of the `.p8` file:
   ```bash
   cat ~/Downloads/AuthKey_XXXXXXXXXX.p8 | pbcopy
   ```
   → paste the copied text as plain text (it starts with `-----BEGIN PRIVATE KEY-----`)

---

### IOS_DIST_SIGNING_KEY_BASE64 + IOS_DIST_SIGNING_KEY_PASSWORD

**What they are:** Your Apple Distribution certificate exported as a `.p12` file — used by CI to code-sign the iOS app.

**Format:** `IOS_DIST_SIGNING_KEY_BASE64` = base64. `IOS_DIST_SIGNING_KEY_PASSWORD` = plain text.

**Step 1 — Check if you have a Distribution certificate:**
1. Open **Xcode → Settings (⌘,) → Accounts**
2. Select your Apple ID → click **Manage Certificates**
3. Look for **Apple Distribution: Karan Sharma**
4. If it's not there: click **+** at the bottom left → **Apple Distribution** → Xcode creates it

**Step 2 — Export from Keychain:**
1. Open **Keychain Access** (search in Spotlight)
2. In the left sidebar select **My Certificates**
3. Find **Apple Distribution: Karan Sharma** (should have a triangle to expand it showing a private key underneath)
4. Right-click on it → **Export "Apple Distribution: Karan Sharma..."**
5. Save as `distribution.p12` to your Desktop
6. Set a strong password when prompted — **write it down**, you need it for the next secret

**Step 3 — Add the secrets:**
```bash
base64 -i ~/Desktop/distribution.p12 | pbcopy
```
→ paste as `IOS_DIST_SIGNING_KEY_BASE64`

→ `IOS_DIST_SIGNING_KEY_PASSWORD` = the password you set in step 6 (plain text)

**Cleanup:**
```bash
rm ~/Desktop/distribution.p12
```

---

### IOS_PROVISIONING_PROFILE_NAME

**What it is:** The exact display name of your App Store provisioning profile. Fastlane uses this to pick the right profile when building. It is NOT a file — just the name string.

**Format:** Plain text.

**Steps:**
1. Go to [developer.apple.com/account/resources/profiles/list](https://developer.apple.com/account/resources/profiles/list)
2. Click **+** to create a new profile
3. Under **Distribution** select **App Store Connect** → Continue
4. Select App ID: `xyz.ksharma.huezoo` → Continue
5. Select your **Apple Distribution** certificate → Continue
6. Profile Name: type `Huezoo App Store` → Generate
7. Click **Download** → double-click the downloaded file to install it into Xcode
8. Add secret: `IOS_PROVISIONING_PROFILE_NAME` = `Huezoo App Store`
   → paste the exact name you typed in step 6 as plain text

---

### GOOGLE_PLAY_SERVICE_ACCOUNT_JSON

**What it is:** A Google service account JSON key that lets CI upload your AAB to Google Play automatically.

**Format:** Plain text — paste the raw JSON content directly, NOT base64.

**Steps:**
1. Go to [play.google.com/console](https://play.google.com/console) → select Huezoo app
2. In the left nav go to **Setup → API access**
3. Click **Link to a Google Cloud project** (use the default project or create new)
4. On the Google Cloud Console page that opens, click **Create service account**:
   - Service account name: `huezoo-ci`
   - Click **Create and continue**
   - Role: skip (click **Continue** → **Done**)
5. Back in Play Console, click **Refresh service accounts**
6. Find `huezoo-ci` in the list → click **Grant access**
   - Role: **Release Manager**
   - Click **Invite user**
7. Now go back to [Google Cloud Console → IAM & Admin → Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts)
8. Click on `huezoo-ci` → **Keys** tab → **Add Key → Create new key → JSON → Create**
9. A `.json` file downloads automatically
10. Open the file in a text editor → select all → copy
    → paste the entire JSON as `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` (plain text, not base64)

---

### FIREBASE_SERVICE_ACCOUNT_KEY

**What it is:** A Firebase service account key used by CI to distribute APKs via Firebase App Distribution (internal testing before Play Store).

**Format:** base64.

**Steps:**
1. Go to [console.firebase.google.com](https://console.firebase.google.com) → Huezoo project
2. Click the **gear icon** (top left) → **Project Settings**
3. Click the **Service accounts** tab
4. Click **Generate new private key** → **Generate key**
5. A `.json` file downloads
6. Base64 encode and add as secret:
```bash
base64 -i ~/Downloads/huezoo-firebase-adminsdk-*.json | pbcopy
```
→ paste as `FIREBASE_SERVICE_ACCOUNT_KEY`

**Cleanup:**
```bash
rm ~/Downloads/huezoo-firebase-adminsdk-*.json
```

---

### FIREBASE_ANDROID_DEBUG_APP_ID + FIREBASE_ANDROID_PROD_APP_ID

**What they are:** Firebase's internal ID for your Android app. Looks like `1:123456789012:android:abcdef1234567890`. Used to target the right app when distributing APKs.

**Format:** Plain text.

**Steps:**
1. Go to [console.firebase.google.com](https://console.firebase.google.com) → Huezoo project
2. Click the **gear icon** → **Project Settings**
3. Scroll down to **Your apps**
4. Click on your Android app
5. Copy the **App ID** field (starts with `1:`)

If you only have one Android app registered in Firebase:
- Use the same value for both `FIREBASE_ANDROID_DEBUG_APP_ID` and `FIREBASE_ANDROID_PROD_APP_ID`

If you want separate debug/release tracking (optional):
- Register a second Android app in Firebase with package name `xyz.ksharma.huezoo.debug`
- Use that App ID for `FIREBASE_ANDROID_DEBUG_APP_ID`

---

## Variables (GitHub Actions Variables tab)

These are not secrets — they are visible plain text values that the workflow reads and updates automatically.

Go to: **Settings → Secrets and variables → Actions → Variables tab → New repository variable**

### ANDROID_VERSION_CODE
- Value: `1`
- The CI increments this automatically on every Play Store release build so you never need to touch it manually.

### IOS_BUILD_NUMBER
- Value: `1`
- The CI increments this automatically on every TestFlight upload.

### DEVELOPMENT_TEAM
- Value: your Apple Developer Team ID
- Where to find it: [developer.apple.com](https://developer.apple.com) → Account → **Membership details** → copy the **Team ID** (10 characters)
- Do NOT put this value in any committed file — Variables tab only.

---

## Verify

Once all secrets and variables are set:

1. Push any commit to `main` → confirm **Huezoo App CI** workflow passes (Detekt + Android debug + iOS build)
2. Manually trigger **Distribute TestFlight** to validate iOS signing end-to-end
3. Run **1. Cut Release Branch** when ready to ship v1.0
