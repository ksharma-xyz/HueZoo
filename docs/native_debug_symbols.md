# Native Debug Symbols — Huezoo Android

Native debug symbols let Google Play symbolicate native crash stack traces in
Android Vitals. Without them, crashes from C/C++ code (e.g. SQLite, Compose
Skia renderer) show raw memory addresses instead of function names.

---

## When to upload

Upload a new symbols zip every time you publish a new release build to the
Play Store. The zip must match the exact build — mismatched symbols produce
garbage stack traces.

---

## How to generate

### 1. Build release

```bash
./gradlew :androidApp:assembleRelease
# or for AAB (Play Store preferred):
./gradlew :androidApp:bundleRelease
```

### 2. Zip and move to Downloads

```bash
python3 scripts/native_debug_symbols.py
# or via alias:
huezooNativeSymbols
```

Output: `~/Downloads/huezoo-native-debug-symbols-<timestamp>.zip`

The script zips all `.so` files under:
```
composeApp/build/intermediates/stripped_native_libs/
  release/stripReleaseDebugSymbols/out/lib/
    armeabi-v7a/
    arm64-v8a/
    x86/
    x86_64/
```

### 3. Upload to Play Console

Play Console → your app → **Android vitals → Deobfuscation files**
→ Upload → select the zip → attach to the correct release/version code.

---

## Aliases (in `~/.zshrc`)

| Alias | What it does |
|-------|-------------|
| `huezooNativeSymbols` | Runs `scripts/native_debug_symbols.py` → zip to Downloads |
| `cleanHuezooDebug` | Clears debug app data on connected device (`adb shell pm clear`) |

---

## Troubleshooting

**"Symbols directory not found"** — release build hasn't run yet, or ran with
`isMinifyEnabled = false`. Make sure `androidApp/build.gradle.kts` has
`isMinifyEnabled = true` and `isShrinkResources = true` in the release block.

**"No .so files found"** — the app has no native libraries in this build
(pure Kotlin/JVM). No upload needed — Play Console will not ask for it.
