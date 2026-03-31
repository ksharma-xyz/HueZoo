# Android Signing — Huezoo

## Key facts

- **Keystore file**: `/Users/ksharma/ksharma-xyz/krail_key.jks`
  (shared with Krail — one file, two aliases)
- **Huezoo alias**: `huezoo-key`
- **Krail alias**: `krail-key`
- **Key algorithm**: RSA 2048-bit, validity 10 000 days
- **Key password**: same as store password (set at creation time — keytool
  did not prompt for a separate key password)

---

## How it works

A `.jks` (Java KeyStore) file is a container that holds one or more
**key pairs**, each identified by an **alias**. Signing with the wrong alias
produces a binary that Play Store rejects.

```
krail_key.jks
  ├── alias: krail-key    → signs xyz.ksharma.krail
  └── alias: huezoo-key   → signs xyz.ksharma.huezoo
```

The `applicationId` (`xyz.ksharma.huezoo`) and the signing key are the two
things Google Play uses to uniquely identify your app. **You cannot change
either after your first upload.**

---

## Build config (`androidApp/build.gradle.kts`)

```kotlin
signingConfigs {
    create("release") {
        storeFile = rootProject.file("keystore.jks")   // symlink → krail_key.jks
        storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("ANDROID_KEY_ALIAS")  // "huezoo-key"
        keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    }
}
```

`keystore.jks` at the repo root is a gitignored symlink:
```bash
ln -s /Users/ksharma/ksharma-xyz/krail_key.jks \
      /Users/ksharma/code/apps/Utility/Huezoo/keystore.jks
```

---

## Local release build

Set env vars in your terminal session (not in `~/.zshrc` — no passwords in
shell profiles):

```bash
export ANDROID_KEYSTORE_PASSWORD="<store_password>"
export ANDROID_KEY_ALIAS="huezoo-key"
export ANDROID_KEY_PASSWORD="<key_password>"   # same as store password
./gradlew :androidApp:assembleRelease
```

---

## CI (GitHub Actions)

The workflow base64-decodes `ANDROID_KEYSTORE_FILE` into `keystore.jks` at
the repo root, then uses the other secrets as env vars.

| GitHub Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_FILE` | `base64 -i krail_key.jks` output |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | `huezoo-key` |
| `ANDROID_KEY_PASSWORD` | key password (same as store) |

Re-encode whenever the keystore file changes:
```bash
base64 -i /Users/ksharma/ksharma-xyz/krail_key.jks | pbcopy
```

---

## Verify / inspect the keystore

```bash
# List all aliases
keytool -list -v -keystore /Users/ksharma/ksharma-xyz/krail_key.jks \
  | grep "Alias name"

# Full details for a specific alias
keytool -list -v -keystore /Users/ksharma/ksharma-xyz/krail_key.jks \
  -alias huezoo-key

# Test a candidate password without changing anything
keytool -list -keystore /Users/ksharma/ksharma-xyz/krail_key.jks \
  -storepass YOUR_CANDIDATE
```

---

## Add a new alias (future apps)

```bash
keytool -genkeypair \
  -keystore /Users/ksharma/ksharma-xyz/krail_key.jks \
  -alias <new-app>-key \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=AppName, O=KSharma, C=IN"
```

---

## Critical reminders

- **Back up `krail_key.jks`** — if lost, you cannot update either app on
  Play Store. Store a copy somewhere outside your Mac (encrypted cloud,
  password manager attachment, etc.).
- **Never commit the `.jks` file** — it is in `.gitignore`.
- **Never change the signing key after first Play Store upload** — Google
  locks the app to that certificate permanently.
